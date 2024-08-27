; ================================================ VOICE LIBRARY (vlib.dll)	By Ne4to
	Include "Source Code\Opus.bb"
	; ================================================ CONSTANTS
		; ================================================ WAVE FILE
			Const WAVE_temp_DIRECTORY$ = "Temp\"
			Const WAVE_FILE_NAME$ = "p.wav"
			Const WAV_HEADER_SIZE = 44
			Const WAV_FORMAT = 1
			Const WAV_BITRATE = 16
			Const WAV_ALIGNMENT = (WAV_FORMAT*(WAV_BITRATE/8))
		; ================================================ VOICE SETTINGS
			Const PLAYERS_UPDATE_VOICE# = 70 ;
			Const MY_UPDATE_RATE# = 0
			Const MAX_PLAYER_GAIN = 12
			if FileType(WAVE_TEMP_DIRECTORY) <> 2 Then CreateDir("Temp")
			
			ClearDirectory("Temp")
			
			Function ClearDirectory(directory$)
				Local d = ReadDir(directory)
				Local filename$
				While True
					filename = NextFile(d)
					if filename = "" Then Exit
					if FileType(directory+"\"+filename) = 1 Then DeleteFile(directory+"\"+filename)
				Wend
			End Function
	; ================================================ TYPES
		Type v_m
			field format_in, codec
			field snd_in
			
			field VoiceInstall, Talking
			Field voicerate
			Field volume#
			Field updateFactor#, prevHold%
			Field selectedinput, drivername$
			
			Field ReceiveStream%
			Field CurrentEncoder%
			Field CurrentDecoder%
		End Type
		Type records
			Field sound%, channel%
			Field volume#, entity%
			Field filename$
			Field waitfactor%
			Field sender
		End Type
	; ================================================ GLOBALS
		Global voice.v_m = New v_m
	; ================================================ INIT
		; ================================================ voice
			voice_init()
		; ================================================ freeze after exit fix
			snd_out_open(snd_format_create(1, 8000, 16, 1))
			snd_out_open(snd_format_create(1, 8000, 16, 1))
			snd_out_open(snd_format_create(1, 8000, 16, 1))
			snd_out_open(snd_format_create(1, 8000, 16, 1))
			snd_out_open(snd_format_create(1, 8000, 16, 1))
			snd_out_open(snd_format_create(1, 8000, 16, 1))
	; ================================================ FUNCTIONS
		Function voice_free()
			if voice\format_in <> 0 Then
				opus_remove_decoder()
				opus_remove_encoder()
				
				voice_stop()
				snd_format_free(voice\format_in)
				voice\format_in = 0
			EndIf
		End Function
		Function voice_create(khz)
			if voice\format_in = 0 Then
				opus_set_sample_rate(khz)
				opus_set_channels(WAV_FORMAT)
				opus_set_default_frame_size(2880)
				
				opus_get_new_encoder()
				opus_get_new_decoder()
				
				voice\format_in = snd_format_create(1, khz, WAV_BITRATE, WAV_FORMAT)
				voice\voicerate = khz
				if (Not voice_start()) Or (Not voice\format_in) Then
					if voice\format_in Then 
						snd_format_free(voice\format_in)
						voice\format_in = 0
					EndIf
					voice\VoiceInstall = False
				EndIf
			EndIf
		End Function
		Function voice_start()
			if (Not voice\snd_in) And voice\format_in <> 0 Then
				voice\snd_in = snd_in_open(voice\format_in, voice_getbytes(), 0)
				debuglog "opened: "+voice_getbytes()
				if voice\snd_in <> 0 Then
					snd_in_start(voice\snd_in)
				EndIf
			EndIf
			Return voice\snd_in
		End Function
		Function voice_stop()
			if voice\snd_in <> 0 Then
				voice_clear(snd_in_readavail(voice\snd_in))
				snd_in_stop(voice\snd_in)
				voice\snd_in = 0
			EndIf
		End Function
		Function voice_changeparameters(khz)
			voice_free()
			voice_create(khz)
		End Function
		Function voice_file_AddWAVHeader(file, writesize) ; header in bank
			; creating wav header
			WriteInt file, tobytes("RIFF")
			WriteInt file, writesize+(WAV_HEADER_SIZE-8)
			WriteInt file, tobytes("WAVE")
			WriteInt file, tobytes("fmt ")
			WriteInt file, 16
			WriteShort file, 1
			WriteShort file, WAV_FORMAT
			WriteInt file, voice\voicerate
			WriteInt file, voice\voicerate*WAV_ALIGNMENT
			WriteShort file, WAV_ALIGNMENT
			WriteShort file, WAV_BITRATE
			WriteInt file, tobytes("data")
			WriteInt file, writesize
		End Function
		Function voice_bank_AddWAVHeader(bank, writesize) ; header in bank
			; creating wav header
			Local vBank = CreateBank(writesize+WAV_HEADER_SIZE)
			PokeInt vBank, 0,tobytes("RIFF")
			PokeInt vBank, 4,writesize+(WAV_HEADER_SIZE-8)
			PokeInt vBank, 8,tobytes("WAVE")
			PokeInt vBank, 12,tobytes("fmt ")
			PokeInt vBank, 16,16
			PokeShort vBank, 20,1
			PokeShort vBank, 22,WAV_FORMAT
			PokeInt vBank, 24,voice\voicerate
			PokeInt vBank, 28,voice\voicerate*WAV_ALIGNMENT
			PokeShort vBank, 32,WAV_ALIGNMENT
			PokeShort vBank, 34,WAV_BITRATE
			PokeInt vBank, 36,tobytes("data")
			PokeInt vBank, 40,writesize
			CopyBank bank, 0, vBank, 44, writesize
			FreeBank bank
			Return vBank
		End Function
		Function voice_recording(holded% = False, playself% = False)
			if voice\VoiceInstall <> 0 Then
				voice\UpdateFactor = Max(0, voice\UpdateFactor-fs\FPSfactor[0])
				if holded = False And voice\prevHold = True Then voice\UpdateFactor = 10*(Not playself)
				if holded = True And voice\prevHold = False Then voice\UpdateFactor = 0
				If holded Then
					voice_render(voice_getbytes(), Not playself)
					voice\talking = True
				Elseif voice\UpdateFactor > 0 Then
					voice_render(voice_getbytes(), Not playself)
				Else
					voice\talking = False
					voice_clear(snd_in_readavail(voice\snd_in))
				EndIf
				voice\PrevHold = holded*(Not playself)
			Else
				voice\talking = False
			EndIf
			if playself Then voice\talking = False
		End Function
		Function voice_render(bytes, NETWORK)
			If snd_in_readavail(voice\snd_in) >= bytes Then
			
				if (float(float(snd_in_readavail(voice\snd_in)) Mod float(opus_get_default_frame_size()*2)) = 0) Then bytes = Min(snd_in_readavail(voice\snd_in), bytes*4)
				vlibbank = CreateBank(bytes)
				snd_in_read(voice\snd_in, vlibbank, banksize(vlibbank))
				voice_send(vlibbank)
				FreeBank vlibbank
				
				if snd_in_readavail(voice\snd_in) >= bytes*16 Then voice_clear(snd_in_readavail(voice\snd_in))
				
				Return True
			EndIf
			Return False
		End Function
		Function voice_send(bank)
			Local Frame_Size = opus_get_default_frame_size()*2
			TempBank% = CreateBank(Frame_Size)
			
			for i = 0 to (banksize(bank)/Frame_Size)-1
				copybank bank, Frame_Size*i, TempBank, 0, Frame_Size
				
				EncodedData% = opus_pcm_encode(TempBank)
				if EncodedData <> 0 Then multiplayer_SendVoice(EncodedData)
				
				FreeBank EncodedData
			next
			
			FreeBank TempBank

		End Function
		Function voice_wave_update()
			For r.records = Each records
			
				if r\Entity <> 0 Then
					UpdateSoundOrigin(r\channel, Camera, r\Entity, 13, 1*2)
				Else
					ChannelVolume r\Channel, 1
				EndIf
				
				if (Not ChannelPlaying(r\channel)) Then
					StopChannel(r\channel)
					FreeSound(r\sound)
					if FileType(r\filename) <> 0 Then DeleteFile(r\filename)
					Delete r
				EndIf
			Next
		End Function
		Function voice_wave_create(sender.Players, dataBank, volume# = 1.0, entity = 0, db = 0)
			Local filename$ = WAVE_temp_DIRECTORY+"p"+sender\ID+".wav"
			While FileType(filename+Str(vc)+".wav") <> 0
				vc = vc + 1
			Wend
			filename = filename+Str(vc)+".wav"

			f = WriteFile(filename)
			if f = 0 Then Return
			voice_file_AddWAVHeader(f, BankSize(dataBank))
			WriteBytes dataBank, f, 0, BankSize(dataBank)
			CloseFile f

			re.records = New records
			re\Entity = entity
			re\volume = volume
			re\filename = filename
			re\sound = LoadSound(filename)
			re\channel = PlaySound(re\sound)
			ChannelVolume re\channel, 0.0

			sender\voicefactor = 30.0

			Return re\sound
		End Function
		Function voice_remove()
			if voice\VoiceInstall Then
				voice_free()
				voice\voiceinstall = False
			EndIf
		End Function
		Function voice_init()
			voice\VoiceInstall = (FileSize("vlib.dll")<>0)
			if voice\VoiceInstall Then voice_create(48000)
		End Function
		Function voice_update()
			voice_recording(voice_isrecording())
			voice_wave_update()
			voice_players_update()
		End Function
		Function voice_isrecording()
			Return KeyDown(KEY_VOICE)
		End Function
		Function voice_player_receive(p.Players, bank, i)
			p\voiceMS = PLAYERS_UPDATE_VOICE ; give wait factor for players
			ResizeBank p\voicebank, BankSize(p\voicebank)+BankSize(bank)
			CopyBank bank, 0, p\voicebank, BankSize(p\voicebank)-BankSize(bank), BankSize(bank) ; add data to player voice bank
		End Function
		Function voice_players_update()
			For p.players = Each players
				if p\voiceMS <= 0 Then
					if voice_player_getavail(p) <> 0 Then
						voice_release_player(p)
						ResizeBank p\voicebank, 0
					EndIf
				Else
					if voice_player_getavail(p) >= (voice_getbytes()*8)/(48000/voice\voicerate) Then
						voice_release_player(p)
						ResizeBank p\voicebank, 0
					Else
						p\voiceMS = p\voiceMS - fs\FPSfactor[0]
					EndIf
				EndIf
			Next
		End Function
		Function voice_release_player(p.players)
			if (p\CurrentRadio = myplayer\CurrentRadio And p\CurrentRadio <> 0) Then voice_wave_create(p, p\voicebank, 1.0, 0, 0)
			if EntityDistance(Collider, p\Pivot) <= 15 Then voice_wave_create(p, p\voicebank, 1.0, p\Pivot, 0)
		End Function
		Function voice_player_getavail(p.players)
			Return BankSize(p\voicebank)
		End Function
		Function voice_clear(avail)
			Local clearbank = CreateBank(avail)
			snd_in_read(voice\snd_in, clearbank, avail)
			FreeBank clearbank
		End Function
		Function voice_getbytes()
			Return opus_get_default_frame_size()*2
		End Function
		Function tobytes(bStr$)
			Local towrite, shlwr
			For i = 1 To Len(bStr)
				towrite = towrite + (Asc(Mid(bStr, i, 1)) Shl shlwr)
				shlwr = shlwr + 8
			Next
			Return towrite
		End Function
; ================================================