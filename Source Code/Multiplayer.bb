; ========================= Constants
Const VERSION$ = "v0.1 Ultimate Edition"

Const STEAM_CONNECT = 125
Const STEAM_TRYCONNECT = 126

Const PACKET_UPDATE = 1
Const PACKET_PING = 2
Const PACKET_CONNECT = 26
Const PACKET_ROTATE = 4
Const PACKET_ALLSYNC = 5
Const PACKET_USEDOOR = 6
Const PACKET_PICKITEM = 7
Const PACKET_DROPITEM = 8
Const PACKET_REMOVEITEM = 9
Const PACKET_FEMURBREAKER = 10
Const PACKET_SINGLEEVENT = 11
Const PACKET_NEWITEM = 12
Const PACKET_VOICE = 13
Const PACKET_DECAL = 14
Const PACKET_ANNOUNCEMENT = 15
Const PACKET_CONNECTERROR = 113

Const PLAYER_SITTING_IDLING = 5
Const PLAYER_SITTING_WALKING_LEFT = 7
Const PLAYER_SITTING_WALKING_RIGHT = 8
Const PLAYER_SITTING_WALKING_BACK = 9
Const PLAYER_SITTING_WALKING_FORWARD = 10
Const PLAYER_IDLING = 11
Const PLAYER_WALKING = 12
Const PLAYER_RUNNING = 13
Const PLAYER_DEAD = 14

Const items_packet = 0
Const events_packet = 1
Const doors_packet = 2
Const npcs_packet = 3
Const roomobjects_packet = 4
; ========================= Main update

Type players
	Field id%
	Field obj, pivot
	field x#, y#, z#, yaw#, pitch#, legyaw#
	field velocity#
	Field playerroomid%
	field wearingnightvision, wearinggasmask, wearingvest, wearinghazmat
	field selecteditem
	field crouchstate#, playersoundvolume#, currentradio, blinktimer#
	field player_move
	field isdead
	field ballisticvest, nvgobj, gasmaskobj
	field ip, port%
	field timeout%
	field received%
	field voicebank, voicems#, voicefactor#
	field RunningTopSpeed#
	Field PlayerBones%[8]
End Type
Type ue_multiplayer
	Field Stream%
	Field Hosted%
	Field CurrentPacket
	Field IP, Port, playerscount
	Field MyID%
	field anticheat%
	field WaitingFactor%
	field availread%
	Field buffer%, bufferreceive%, bufferwrited%, bufferreaded%
	Field CSteamID_Buff%
	Field CurrentLobby
	Field msgip, msgport
End Type

Include "Source Code\voice.bb"

global multiplayer.ue_multiplayer = New ue_multiplayer
global myplayer.players
global ShouldSendItem% = False
global multiplayerError$
global multiplayerErrorTime%
Global SavedPoses$[5]

multiplayer\buffer = CreateBank(8192)
multiplayer\bufferreceive = CreateBank(8192)
multiplayer\CSteamID_Buff = BS_CSteamID_New()

Function multiplayer_Update()
	steam_update()
	if multiplayer\stream <> 0 then
		while steamrecv()
			multiplayer_getpacket(multiplayer_ReadByte(), multiplayer\recv)
		wend
		multiplayer\playerscount = 0
		for p.players = each players
			multiplayer\playerscount = multiplayer\playerscount+1
		next
		
		multiplayer_UpdateCore()
		voice_update()
	endif
end function

function convertdifficult(name$)
	select lower(name)
		case "safe"
			return 0
		case "euclid"
			return 1
		case "keter"
			return 2
		case "thaumiel"
			return 3
	end select
end function

Function multiplayer_UDPMsgIP()
	Return multiplayer\msgip
End Function
Function multiplayer_UDPMsgPort()
	Return multiplayer\msgport
End Function

Function multiplayer_GetPacket(byte, ip, port = 0)
	Local floatx#, floaty#, floatz#, floatyaw#, floatpitch#, floatroll#, floatscale#
	
	if multiplayer_IsAHost() then
		select byte
			Case STEAM_TRYCONNECT
				local fp 
				For p.players = Each players
					if p\ID <> NetworkServer\MyID THEN
						if BS_CSteamID_GetAccountID(multiplayer_UDPMsgIP()) = BS_CSteamID_GetAccountID(p\IP) Then
							fp = true
							exit
						EndIf
					Endif
				Next
				if not fp then
					multiplayer_WriteByte STEAM_CONNECT
					multiplayer_WriteByte 2
					multiplayer_SendMessage(multiplayer\msgip)
				endif
			Case PACKET_SOUND
				id = multiplayer_ReadByte()
				For p.players = each players
					if p\ID = id then
						otherindexstr$ = multiplayer_ReadLine()
						floatx = multiplayer_ReadFloat()
						floaty = multiplayer_ReadFloat()
						
						if otherindexstr = "SFX\SCP\513\Bell1.ogg" Then 
							If Curr5131 = Null
								Curr5131 = CreateNPC(NPCtype5131, 0,0,0)
							EndIf
						EndIf
						
						if otherindexstr <> "" Then
						
							For p2.players = Each players
								if p2 <> myplayer And p2 <> p then
									multiplayer_WriteByte PACKET_SOUND
									multiplayer_WriteByte p\ID
									multiplayer_WriteLine otherindexstr
									multiplayer_WriteFloat floatx
									multiplayer_WriteFloat floaty
									
									multiplayer_SendMessage(multiplayer_getip(p2), multiplayer_getport(p2))
								EndIf
							Next
							
							For snd.sound = Each sound
								if snd\name = otherindexstr Then
									Play3DSound(Handle(snd), Camera, p\Pivot, floatx, floaty)
									otherindexstr = ""
									Exit
								EndIf
							Next
							if otherindexstr <> "" Then Play3DSound(0, Camera, p\Pivot, floatx, floaty, otherindexstr)
						EndIf
						exit
					endif
				next
			Case PACKET_DECAL
				id = multiplayer_ReadByte()
				pers% = multiplayer_ReadByte()
				pers2# = multiplayer_ReadFloat()
				pers3# = multiplayer_ReadFloat()
				pers4# = multiplayer_ReadFloat()
				pers5# = multiplayer_ReadFloat()
				pers6# = multiplayer_ReadFloat()
				pers7# = multiplayer_ReadFloat()
				pers8# = multiplayer_ReadFloat()
				pers9# = multiplayer_ReadFloat()
				pers10# = multiplayer_ReadFloat()
				pers11# = multiplayer_ReadFloat()
				pers12# = multiplayer_ReadFloat()
				pers13# = multiplayer_ReadFloat()
				des.decals = CreateDecal(pers%, pers2#, pers3#, pers4#, pers7#, pers5#, pers6#)
				if pers = 5 Then EntityColor(des\obj, 0.0, Rnd(200, 255), 0.0)
				des\SizeChange = pers8#
				des\Size = pers9#
				des\MaxSize = pers10#
				des\AlphaChange = pers11#
				des\Alpha = pers12#
				des\timer = pers13#
				EntityAlpha(des\obj, des\Alpha)
				ScaleSprite(des\obj,des\Size,des\Size)
				For p.players = Each players
					if p\ID <> myplayer\ID And p\ID <> id Then
						multiplayer_WriteByte PACKET_DECAL
						multiplayer_WriteByte pers
						multiplayer_WriteFloat pers2
						multiplayer_WriteFloat pers3
						multiplayer_WriteFloat pers4
						multiplayer_WriteFloat pers5
						multiplayer_WriteFloat pers6
						multiplayer_WriteFloat pers7
						multiplayer_WriteFloat pers8
						multiplayer_WriteFloat pers9
						multiplayer_WriteFloat pers10
						multiplayer_WriteFloat pers11
						multiplayer_WriteFloat pers12
						multiplayer_WriteFloat pers13
						multiplayer_SendMessage(multiplayer_getip(p), multiplayer_getport(p))
					Endif
				Next
			Case PACKET_VOICE
				bnk = 0
				id = multiplayer_ReadByte()
				For p.players = each players
					if p\ID = id then
						bnk = CreateBank(ReadAvail(multiplayer_GetStream()))
						multiplayer_ReadBytes(bnk, 0, BankSize(bnk))
						
						For p2.players = Each players
							if p2 <> myplayer And p2 <> p then
								multiplayer_WriteByte PACKET_VOICE
								multiplayer_WriteByte p\ID
								multiplayer_WriteBytes bnk, 0, BankSize(bnk)
								multiplayer_SendMessage(multiplayer_getip(p2), multiplayer_getport(p2))
							endif
						next
						
						pcm = opus_pcm_decode(bnk)
						if pcm <> 0 then
							voice_player_receive(p, pcm, 0)
							freebank pcm
						endif
						Exit
					EndIf
				Next
			Case PACKET_NEWITEM
				id = multiplayer_ReadByte()
				itnameid = multiplayer_ReadByte()
				For its.ItemTemplates = Each ItemTemplates
					if its\templateID = itnameID Then
						For p.players = Each players
							if p\ID = id then
								it.Items = CreateItem(its\name, its\tempname, EntityX(p\Pivot),EntityY(p\Pivot)+0.38,EntityZ(p\Pivot))
								EntityType (it\collider, HIT_ITEM)
								it\picker = 0
								exit
							endif
						Next
						Exit
					EndIf
				Next
			Case PACKET_SINGLEEVENT
				eventID = multiplayer_ReadByte()
				For e.Events = Each events
					if e\ID = eventID then
						e\EventState = multiplayer_ReadFloat()
						e\EventState2 = multiplayer_ReadFloat()
						e\EventState3 = multiplayer_ReadFloat()
						Exit
					EndIf
				Next
			Case PACKET_FEMURBREAKER
				id = multiplayer_ReadByte()
				For e.Events = Each events
					if e\EventConst = e_room106 then
						e\EventState = 1 ;start the femur breaker
						If SoundTransmission = True Then ;only play sounds if transmission is on
							If e\SoundCHN2 <> 0 Then
								If ChannelPlaying(e\SoundCHN2) Then StopChannel e\SoundCHN2
							EndIf 
							FemurBreakerSFX = LoadSound_Strict(SFXPath$+"Room\106Chamber\FemurBreaker.ogg")
							e\SoundCHN2 = PlaySound_Strict(FemurBreakerSFX)
						EndIf
						Exit
					EndIf
				Next
				multiplayer_Send(PACKET_FEMURBREAKER, id)
			case PACKET_PING
				multiplayer_WriteByte(1)
				multiplayer_SendMessage(ip, port)
			Case PACKET_CONNECT
				multiplayer_ReadByte()
				multiplayer_ReadLine()
				if multiplayer_ReadLine() = VERSION Then
					p.players = multiplayer_CreatePlayer()		
					multiplayer_WriteByte PACKET_CONNECT
					multiplayer_WriteByte p\ID
					multiplayer_WriteLine RandomSeed
					multiplayer_WriteByte convertdifficult(SelectedDifficulty\name)
					multiplayer_WriteByte multiplayer\anticheat
					multiplayer_WriteByte introenabled
					
					p\IP = ip
					p\Port = ports
					p\timeout = MilliSecs()+120000
					
					multiplayer_SendMessage(ip, port)
					if Not IntroEnabled Then
						for r.rooms = each rooms
							if r\roomtemplate\name = "room173" then
								p\x = EntityX(r\obj)+3584*RoomScale
								p\y = (704*RoomScale)-0.9
								p\z = EntityZ(r\obj)+1024*RoomScale
								p\yaw = 130.3
								;p\legyaw = wrapangle(130.3-180)
								p\playerroomid = r\Id
								p\player_move = PLAYER_IDLING
								exit
							endif
						next
					Else
						for r.rooms = each rooms
							if r\roomtemplate\name = "room173intro" then
								p\x = EntityX(r\obj)
								p\y = 1.0
								p\z = EntityZ(r\obj)
								p\yaw = 130.3
								;p\legyaw = wrapangle(130.3-180)
								p\playerroomid = r\Id
								p\player_move = PLAYER_IDLING
								exit
							endif
						next
					EndIf
				Else
					multiplayer_WriteByte PACKET_CONNECTERROR
					multiplayer_WriteLine "Version doesn't match"
					multiplayer_SendMessage(ip, port)
				EndIf
			Case PACKET_UPDATE
				id = multiplayer_ReadByte()
				for p.players = each players
					if p\id = id then
						p\timeout = MilliSecs()+120000
						p\x = multiplayer_ReadFloat()
						p\y = multiplayer_ReadFloat()
						p\z = multiplayer_ReadFloat()
						p\yaw = multiplayer_ReadFloat()
						p\legyaw = multiplayer_ReadFloat()
						p\pitch = multiplayer_ReadFloat()
						p\blinktimer = multiplayer_ReadFloat()
						p\crouchstate = multiplayer_ReadByte()
						p\playersoundvolume = multiplayer_ReadByte()
						p\selecteditem = multiplayer_ReadByte()
						p\velocity = multiplayer_ReadFloat()
						p\playerroomid = multiplayer_ReadByte()
						wearingdir = multiplayer_ReadByte()
						p\PLAYER_MOVE = multiplayer_ReadByte()
						p\currentradio = multiplayer_ReadByte()
						p\wearingnightvision = ReadBool(wearingdir, 0)
						p\wearingvest = ReadBool(wearingdir, 1)
						p\wearinggasmask = ReadBool(wearingdir, 2)
						p\wearinghazmat = ReadBool(wearingdir, 3)
						exit
					endif
				next
			Case PACKET_ROTATE
				id = multiplayer_ReadByte()
				for p.players = each players
					if p\id = id then
						otherindex = multiplayer_ReadByte()
						floatx# = multiplayer_ReadFloat()
						floaty# = multiplayer_ReadFloat()
						floatz# = multiplayer_ReadFloat()
						floatpitch# = multiplayer_ReadFloat()
						floatyaw# = multiplayer_ReadFloat()
						floatroll# = multiplayer_ReadFloat()
						For r.Rooms = Each Rooms
							if r\ID = p\PlayerRoomID Then
								PositionEntity r\Objects[otherindex], floatx,floaty,floatz, True
								RotateEntity(r\Objects[otherindex], floatpitch, floatyaw, floatroll, True)
								r\ObjectUsed[otherindex] = True
								Exit
							EndIf
						Next
						Exit
					endif
				next
			Case PACKET_DROPITEM
				id = multiplayer_ReadByte()
				For p.players = each players
					if p\ID = id then
						otherindex = multiplayer_ReadShort()
						For it.Items = Each Items
							If it\ID = otherindex And it\picker = ID Then
								it\picker = 0
								it\Picked = False
								ShowEntity(it\collider)
								
								PositionEntity(it\collider, EntityX(p\pivot), EntityY(p\pivot)+0.38, EntityZ(p\pivot))
								ResetEntity it\Collider
								RotateEntity(it\collider, EntityPitch(p\pivot), (EntityYaw(p\pivot)-180)+Rnd(-20,20), 0)
								MoveEntity(it\collider, 0, -0.1, 0.1)
								RotateEntity(it\collider, 0, (EntityYaw(p\pivot)-180)+Rnd(-110,110), 0)
								If (it\itemtemplate\tempname="clipboard") Then
									it\invimg = it\itemtemplate\invimg2
									SetAnimTime it\model,17.0
								ElseIf (it\itemtemplate\tempname="wallet") Then
									SetAnimTime it\model,0.0
								EndIf
								
								Exit
							EndIf
						Next
						Exit
					EndIf
				next
			Case PACKET_PICKITEM
				id = multiplayer_ReadByte()
				otherindex = multiplayer_ReadShort()
				For it.Items = Each Items
					If it\ID = otherindex And it\picker = 0 Then
						it\picker = ID
						HideEntity it\collider
						it\Picked = True
						Exit
					EndIf
				Next
			Case PACKET_REMOVEITEM
				id = multiplayer_ReadByte()
				otherindex = multiplayer_ReadShort()
				For it.Items = Each Items
					If it\ID = otherindex And it\picker = id Then
						RemoveItem(it, False)
						Exit
					EndIf
				Next
			Case PACKET_USEDOOR
				id = multiplayer_ReadByte()
				otherindex = multiplayer_ReadShort()
				otherindex2 = multiplayer_ReadByte()
				For d.Doors = Each Doors
					If d\ID = otherindex Then
						d\locked = multiplayer_ReadByte()
						if d\open <> otherindex2 Then
							prevmsg$ = Msg
							prevmsgtimer = MsgTimer
							prevselecteditem.Items = SelectedItem
							UseDoor(d, True, True, False)
							Msg = prevMsg
							MsgTimer = prevmsgtimer
							SelectedItem = prevselecteditem
						EndIf
						Exit
					EndIf
				Next
				if d <> Null Then
					For p.players = Each players
						if p\ID <> myplayer\ID and p\ID <> id Then
							multiplayer_WriteByte PACKET_USEDOOR
							multiplayer_WriteShort otherindex
							multiplayer_WriteByte otherindex2
							multiplayer_WriteByte d\locked
							multiplayer_SendMessage(multiplayer_getip(p), multiplayer_getport(p))
						EndIf
					Next
				EndIf
		end select
	else
		select byte
			Case PACKET_ANNOUNCEMENT
				if PlayerInReachableRoom() Then PlayAnnouncement(multiplayer_ReadLine())
			Case PACKET_SOUND
				id = multiplayer_ReadByte()
				For p.players = each players
					if p\ID = id then
						otherindexstr$ = multiplayer_ReadLine()
						floatx = multiplayer_ReadFloat()
						floaty = multiplayer_ReadFloat()
						
						if otherindexstr = "SFX\SCP\513\Bell1.ogg" Then 
							If Curr5131 = Null
								Curr5131 = CreateNPC(NPCtype5131, 0,0,0)
							EndIf
						EndIf
						
						if otherindexstr <> "" Then
							For snd.sound = Each sound
								if snd\name = otherindexstr Then
									Play3DSound(Handle(snd), Camera, p\Pivot, floatx, floaty)
									otherindexstr = ""
									Exit
								EndIf
							Next
							if otherindexstr <> "" Then Play3DSound(0, Camera, p\Pivot, floatx, floaty, otherindexstr)
						EndIf
						exit
					endif
				next
			Case PACKET_DECAL
				pers% = multiplayer_ReadByte()
				pers2# = multiplayer_ReadFloat()
				pers3# = multiplayer_ReadFloat()
				pers4# = multiplayer_ReadFloat()
				pers5# = multiplayer_ReadFloat()
				pers6# = multiplayer_ReadFloat()
				pers7# = multiplayer_ReadFloat()
				pers8# = multiplayer_ReadFloat()
				pers9# = multiplayer_ReadFloat()
				pers10# = multiplayer_ReadFloat()
				pers11# = multiplayer_ReadFloat()
				pers12# = multiplayer_ReadFloat()
				pers13# = multiplayer_ReadFloat()
				des.decals = CreateDecal(pers%, pers2#, pers3#, pers4#, pers7#, pers5#, pers6#)
				if pers = 5 Then EntityColor(des\obj, 0.0, Rnd(200, 255), 0.0)
				des\SizeChange = pers8#
				des\Size = pers9#
				des\MaxSize = pers10#
				des\AlphaChange = pers11#
				des\Alpha = pers12#
				des\timer = pers13#
				EntityAlpha(des\obj, des\Alpha)
				ScaleSprite(des\obj,des\Size,des\Size)
			Case PACKET_VOICE
				bnk = 0
				id = multiplayer_ReadByte()
				For p.players = each players
					if p\ID = id then
						bnk = CreateBank(ReadAvail(multiplayer_GetStream()))
						multiplayer_ReadBytes(bnk, 0, BankSize(bnk))
						
						pcm = opus_pcm_decode(bnk)
						if pcm <> 0 then
							voice_player_receive(p, pcm, 0)
							freebank pcm
						endif
						FreeBank bnk
						Exit
					EndIf
				Next
			Case PACKET_USEDOOR
				otherindex = multiplayer_ReadShort()
				otherindex2 = multiplayer_ReadByte()
				For d.Doors = Each Doors
					If d\ID = otherindex Then
						d\locked = multiplayer_ReadByte()
						if d\open <> otherindex2 or d\IsElevatorDoor = 0 Then
							prevmsg$ = Msg
							prevmsgtimer = MsgTimer
							prevselecteditem.Items = SelectedItem
							UseDoor(d, True, True, False)
							Msg = prevMsg
							MsgTimer = prevmsgtimer
							SelectedItem = prevselecteditem
						EndIf
						Exit
					EndIf
				Next
			Case PACKET_FEMURBREAKER
				For e.Events = Each events
					if e\EventConst = e_room106 then
						e\EventState = 1 ;start the femur breaker
						If SoundTransmission = True Then ;only play sounds if transmission is on
							If e\SoundCHN2 <> 0 Then
								If ChannelPlaying(e\SoundCHN2) Then StopChannel e\SoundCHN2
							EndIf 
							FemurBreakerSFX = LoadSound_Strict(SFXPath$+"Room\106Chamber\FemurBreaker.ogg")
							e\SoundCHN2 = PlaySound_Strict(FemurBreakerSFX)
						EndIf
						Exit
					EndIf
				Next
			Case PACKET_UPDATE
				while true
					id = multiplayer_ReadByte()
					if id = 0 then exit
					p.players = multiplayer_createplayer(id)
					p\x = multiplayer_ReadFloat()
					p\y = multiplayer_ReadFloat()
					p\z = multiplayer_ReadFloat()
					p\yaw = multiplayer_ReadFloat()
					p\legyaw = multiplayer_ReadFloat()
					p\pitch = multiplayer_ReadFloat()
					p\blinktimer = multiplayer_ReadFloat()
					p\crouchstate = multiplayer_ReadByte()
					p\playersoundvolume = multiplayer_ReadByte()
					p\selecteditem = multiplayer_ReadByte()
					p\velocity = multiplayer_ReadFloat()
					p\playerroomid = multiplayer_ReadByte()
					wearingdir = multiplayer_ReadByte()
					p\PLAYER_MOVE = multiplayer_ReadByte()
					p\currentradio = multiplayer_ReadByte()
					p\wearingnightvision = ReadBool(wearingdir, 0)
					p\wearingvest = ReadBool(wearingdir, 1)
					p\wearinggasmask = ReadBool(wearingdir, 2)
					p\wearinghazmat = ReadBool(wearingdir, 3)
					p\received = true
				wend
				for p.players = each players
					if p <> myplayer then
						if p\received = false then 
							multiplayer_removeplayer(p)
						Else
							p\received = false
						EndIf
					endif
				next
			Case PACKET_ALLSYNC
				Select multiplayer_ReadByte()
					Case npcs_packet
						if QuickLoadPercent = -1 Or QuickLoadPercent = 100 Then
							Contained106 = multiplayer_ReadByte()
							chs\NoTarget = multiplayer_ReadByte()
							While True
								otherindex = multiplayer_ReadByte()
								if otherindex = 0 Then Exit
								otherindex2 = multiplayer_ReadByte()
								n.NPCs = NPC[otherindex]
								if n = Null Then
									n = CreateNPC(otherindex2, 0,0,0)
									SetNPCID(n, otherindex)
								EndIf
								ResetNPC(n, otherindex2)
								n\waitFactor = 70*NPC_WAIT_FACTOR
								n\Idle = multiplayer_ReadByte()
								n\State = multiplayer_ReadFloat()
								n\State2 = multiplayer_ReadFloat()
								n\State3 = multiplayer_ReadFloat()
								n\x = multiplayer_ReadFloat()
								n\y = multiplayer_ReadFloat()
								n\z = multiplayer_ReadFloat()
								n\yaw = multiplayer_ReadFloat()
								n\mpframe = multiplayer_ReadFloat()
								
								ChangeNPCTextureID(n, multiplayer_ReadByte()-1)
								n\EventID = multiplayer_ReadByte()
								n\NPCEventID = multiplayer_ReadByte()
								n\Target = NPC[multiplayer_ReadByte()]
								if Event[n\EventID] <> Null Then Event[n\EventID]\room\NPC[n\NPCEventID] = n
								Select n\NPCtype
									Case NPCtype173: Curr173 = n
									Case NPCtype096: Curr096 = n
									Case NPCtypeOldman: curr106 = n
									Case NPCtype5131: curr5131 = n
								End Select
							Wend
							For n.NPCs = Each NPCs
								if n\waitFactor < 1 Then
									if Event[n\EventID] <> Null Then RemoveEvent(Event[n\EventID])
									RemoveNPC(n)
								Else
									if n\waitFactor <> 70*NPC_WAIT_FACTOR Then n\x = 999 : n\y = 999 : n\z = 999 ; deactivate npc if he cant received
									n\waitFactor = n\waitFactor - fs\FPSfactor[0]
								EndIf
							Next
						EndIf
					Case items_packet
						While True
							otherindex = multiplayer_ReadShort()
							if otherindex = 0 Then Exit
							it.Items = Item[otherindex]
							otherindex2 = multiplayer_ReadByte()
							floatx# = multiplayer_ReadFloat()
							floaty# = multiplayer_ReadFloat()
							floatz# = multiplayer_ReadFloat()
							otherindex3 = multiplayer_ReadByte()
							If it = Null Then
								For its.ItemTemplates = Each ItemTemplates
									if its\templateID = otherindex2 Then
										it = CreateItemByTemplate(its,1,1,1)
										EntityType it\collider, HIT_ITEM
										Exit
									EndIf
								Next
							Elseif it\itemtemplate\templateID <> otherindex2
								RemoveItem(it, False)
								For its.ItemTemplates = Each ItemTemplates
									if its\templateID = otherindex2 Then
										it = CreateItemByTemplate(its,1,1,1)
										EntityType it\collider, HIT_ITEM
										Exit
									EndIf
								Next
							EndIf
							if it <> Null Then
								SetItemID(it, otherindex)
								it\waitFactor = True
								it\x = floatx#
								it\y = floaty#
								it\z = floatz#
								it\picker = otherindex3
								it\picked = (it\picker<>0)
							EndIf
						Wend
						For it.Items = Each Items
							if Not it\waitFactor Then
								RemoveItem(it, False)
							Else
								it\waitFactor = False
							EndIf
						Next
					Case doors_packet
						While True
							otherindex = multiplayer_ReadShort()
							if otherindex = 0 Then Exit
							d.Doors = Door[otherindex]
							if d <> Null Then
								otherindex2 = multiplayer_ReadByte()
								if d\IsElevatorDoor = 0 Then
									d\open = ReadBool(otherindex2, 0)
									d\locked = ReadBool(otherindex2, 1)
								EndIf
							Else
								multiplayer_ReadByte()
							EndIf
						Wend
					Case events_packet
						if QuickLoadPercent = -1 Or QuickLoadPercent = 100 Then
							While True
								otherindex = multiplayer_ReadByte()
								if otherindex = 0 Then Exit
								e.Events = Event[otherindex]
								otherindex2 = multiplayer_ReadByte()
								otherindex3 = multiplayer_ReadByte()
								floatx# = multiplayer_ReadFloat()
								floaty# = multiplayer_ReadFloat()
								floatz# = multiplayer_ReadFloat()
								if e <> null Then
									e\Received = 70
									e\UsedEvent = False
									if floatx > e\EventState Then e\EventState = floatx
									if floaty > e\EventState2 Then e\EventState2 = floaty
									if floatz > e\EventState3 Then e\EventState3 = floatz
								EndIf
							Wend
							For e.Events = Each Events
								if e\Received <= 0.0 And (Not IsABlockedEvent(e)) And (Not IsANotRemovedEvent(e)) Then
									RemoveEvent(e)
								Else
									e\Received = e\Received-fs\FPSfactor[0]
								EndIf
							Next
						EndIf
					Case roomobjects_packet
						While True
							otherindex = multiplayer_ReadByte()
							if otherindex = 0 Then Exit
							r.Rooms = Room[otherindex]
							otherindex2 = multiplayer_ReadByte()
							
							floatx# = multiplayer_ReadFloat()
							floaty# = multiplayer_ReadFloat()
							floatz# = multiplayer_ReadFloat()
							
							floatyaw# = multiplayer_ReadFloat()
							floatpitch# = multiplayer_ReadFloat()
							floatroll# = multiplayer_ReadFloat()
							if r <> Null Then
								if GrabbedEntity <> r\Objects[otherindex2] Then
									if r\Objects[otherindex2] <> 0 Then
										PositionEntity(r\Objects[otherindex2], floatx, floaty, floatz, True)
										RotateEntity(r\Objects[otherindex2], floatyaw, floatpitch, floatroll, True)
									EndIf
								EndIf
								r\ObjectUsed[otherindex2] = True
							EndIf
						Wend
				end select
		end select
	endif
End Function

Function multiplayer_UpdateCore()
	if multiplayer_IsAHost() then
		if multiplayer\WaitingFactor < MilliSecs() Then
			multiplayer_UpdateRoomObjects()
			
			for p.players = each players
				if p\ID <> myplayer\ID And p\timeout > MilliSecs()+118000 then
					multiplayer_WriteByte(PACKET_UPDATE)
					for p2.players = each players
						if p2 <> p then
							multiplayer_WriteByte(p2\ID)
							multiplayer_WriteFloat(p2\x)
							multiplayer_WriteFloat(p2\y)
							multiplayer_WriteFloat(p2\z)
							multiplayer_WriteFloat(p2\yaw)
							multiplayer_WriteFloat(p2\legyaw)
							multiplayer_WriteFloat(p2\pitch)
							multiplayer_WriteFloat(p2\blinktimer)
							multiplayer_WriteByte(p2\crouchstate)
							multiplayer_WriteByte(p2\playersoundvolume)
							multiplayer_WriteByte(p2\selecteditem)
							multiplayer_WriteFloat(p2\velocity)
							multiplayer_WriteByte(p2\playerroomid)
							multiplayer_WriteByte((p2\wearingnightvision)+(2*p2\wearingvest)+(4*p2\wearinggasmask)+(8*p2\wearinghazmat))
							multiplayer_WriteByte(p2\PLAYER_MOVE)
							multiplayer_WriteByte(p2\CurrentRadio)
						EndIf
					next
					multiplayer_SendMessage(multiplayer_getip(p), multiplayer_getport(p))
				endif
			next
			multiplayer\WaitingFactor = MilliSecs()+30
		EndIf
		; ================================================================================== Game
		for p.players = each players
			if p\ID <> myplayer\ID And p\timeout > MilliSecs()+118000 then
				for a = 0 to 4
					multiplayer_WriteByte(PACKET_ALLSYNC)
					multiplayer_WriteByte(a)
					
					select a
						case items_packet ; Items
							Local toSend = False
							For it.Items = Each Items
								if it\picker <> 0 Then
									for p2.players = each players
										if p2\ID = it\picker then
											if p2\SelectedItem = it\ID Then toSend = True
										endif
									next
								EndIf
								if EntityDistance(it\collider, p\Pivot) < HideDistance*0.5 Or it\picker = p\ID Or toSend Then
									multiplayer_WriteShort(it\ID)
									multiplayer_WriteByte(it\itemtemplate\templateID)
									multiplayer_WriteFloat(EntityX(it\collider, True))
									multiplayer_WriteFloat(EntityY(it\collider, True))
									multiplayer_WriteFloat(EntityZ(it\collider, True))
									multiplayer_WriteByte(it\picker)
								EndIf
								toSend = False
							Next
							multiplayer_WriteShort(0)
							
						case events_packet
							For e.Events = Each Events
								if Not IsABlockedEvent(e) Then
									multiplayer_WriteByte e\ID
									multiplayer_WriteByte e\EventConst
									multiplayer_WriteByte e\room\ID
									multiplayer_WriteFloat e\EventState
									multiplayer_WriteFloat e\EventState2
									multiplayer_WriteFloat e\EventState3
								EndIf
							Next
							multiplayer_WriteByte 0
						case doors_packet
							For d.Doors = Each Doors
								if EntityDistance(d\obj, p\Pivot) < HideDistance And d\IsElevatorDoor = 0 Then
									multiplayer_WriteShort(d\ID)
									multiplayer_WriteByte((d\open) + (2 * d\locked))
								EndIf
							Next
							multiplayer_WriteShort(0)
						case npcs_packet
							multiplayer_WriteByte Contained106
							multiplayer_WriteByte chs\NoTarget
							For n.NPCs = Each NPCs
								if n\EventID <> 0 Or EntityDistance(n\Collider, p\Pivot) < HideDistance Or Curr106 = n Or Curr5131 = n Or Curr096 = n Or Curr173 = n Or Curr066 = n Then
									multiplayer_WriteByte(n\ID)
									multiplayer_WriteByte(n\NPCtype)
									multiplayer_WriteByte(n\Idle)
									
									multiplayer_WriteFloat(n\State)
									multiplayer_WriteFloat(n\State2)
									multiplayer_WriteFloat(n\State3)
									
									multiplayer_WriteFloat(EntityX(n\Collider,True))
									multiplayer_WriteFloat(EntityY(n\Collider,True))
									multiplayer_WriteFloat(EntityZ(n\Collider,True))
									multiplayer_WriteFloat(EntityYaw(n\Collider, True))
									multiplayer_WriteFloat(AnimTime(n\obj))
									
									multiplayer_WriteByte(n\TextureID)
									multiplayer_WriteByte(n\EventID)
									multiplayer_WriteByte(n\NPCEventID)
									if n\Target <> Null Then multiplayer_WriteByte(n\Target\ID) Else multiplayer_WriteByte 0
								EndIf
							Next
							multiplayer_WriteByte 0
						case roomobjects_packet
							For r.Rooms = Each Rooms
								if Not IsABlockedRoom(r) Then
									For i = 0 To MaxRoomObjects-1
										if r\Objects[i] <> 0 Then 
											if r\ObjectUsed[i] = True Then
												multiplayer_WriteByte r\ID
												multiplayer_WriteByte(i)
												multiplayer_WriteFloat(EntityX(r\Objects[i],True))
												multiplayer_WriteFloat(EntityY(r\Objects[i],True))
												multiplayer_WriteFloat(EntityZ(r\Objects[i],True))
												multiplayer_WriteFloat(EntityPitch(r\Objects[i], True))
												multiplayer_WriteFloat(EntityYaw(r\Objects[i], True))
												multiplayer_WriteFloat(EntityRoll(r\Objects[i], True))
											EndIf
										EndIf
									Next
								EndIf
							Next
							multiplayer_WriteByte(0)
					end select
					
					multiplayer_SendMessage(multiplayer_getip(p), multiplayer_getport(p))
				next
			endif
		next
		;multiplayer\currentpacket = flipvalue(multiplayer\currentpacket+1, 5)
		
	else
		if multiplayer\WaitingFactor < MilliSecs() Then
			multiplayer_UpdateRoomObjects()
			
			multiplayer_WriteByte(PACKET_UPDATE)
			multiplayer_WriteByte(myplayer\ID)
			multiplayer_WriteFloat(myplayer\x)
			multiplayer_WriteFloat(myplayer\y)
			multiplayer_WriteFloat(myplayer\z)
			multiplayer_WriteFloat(myplayer\yaw)
			multiplayer_WriteFloat(myplayer\legyaw)
			multiplayer_WriteFloat(myplayer\pitch)
			multiplayer_WriteFloat(myplayer\blinktimer)
			multiplayer_WriteByte(myplayer\crouchstate)
			multiplayer_WriteByte(myplayer\playersoundvolume)
			multiplayer_WriteByte(myplayer\selecteditem)
			multiplayer_WriteFloat(myplayer\velocity)
			multiplayer_WriteByte(myplayer\playerroomid)
			multiplayer_WriteByte((myplayer\wearingnightvision)+(2*myplayer\wearingvest)+(4*myplayer\wearinggasmask)+(8*myplayer\wearinghazmat))
			multiplayer_WriteByte(myplayer\PLAYER_MOVE)
			multiplayer_WriteByte(myplayer\CurrentRadio)
			
			multiplayer_SendMessage(multiplayer_getip(null), multiplayer_getport(null))
			multiplayer\WaitingFactor = MilliSecs()+30
			
			;DebugLog "send "+MilliSecs()
		EndIf
	endif
End Function

Function multiplayer_IsAHost()
	return multiplayer\hosted
end function

Function multiplayer_error(mess$)
	multiplayerError$ = mess$
	multiplayerErrorTime% = 70*3
end function

function multiplayer_starthost(port)
	BS_SteamMatchmaking_CreateLobby(BS_SteamMatchmaking(), multiplayer\CurrentLobbyType, multiplayer\Maxplayers)
	multiplayer\port = port
	multiplayer\hosted = True
	multiplayer\MyID = 1
	return multiplayer\stream
end function

function multiplayer_connect()
	multiplayer\IP = multiplayer\recv
	multiplayer\Port = 0
	
	multiplayer_WriteByte(PACKET_CONNECT)
	multiplayer_WriteByte(0)
	multiplayer_WriteLine("Null")
	multiplayer_WriteLine(VERSION)
	multiplayer_SendMessage(multiplayer\IP, multiplayer\Port)
	WaitingFactor% = MilliSecs()+3000
	while WaitingFactor > MilliSecs()
		steam_update()
		if steamrecv() then
			select multiplayer_ReadByte()
				case PACKET_CONNECT
					multiplayer\MyID = multiplayer_ReadByte()
					randomseed$ = multiplayer_ReadLine()
					SelectedDifficulty = difficulties(multiplayer_readbyte())
					
					multiplayer\anticheat = multiplayer_readbyte()
					introenabled = multiplayer_readbyte()
					Return True
				Case PACKET_CONNECTERROR
					multiplayer_error(multiplayer_ReadLine())
			End select
		endif
		Delay 10
	wend
	
	multiplayer_close()
	Return False
end function

function multiplayer_close()
	if multiplayer\CurrentLobby <> 0 Then BS_SteamMatchmaking_LeaveLobby(BS_SteamMatchmaking(), multiplayer\CurrentLobby)

	if not multiplayer\hosted then
		BS_CSteamID_Set(BS_SteamID_Dynamic, BS_CSteamID_GetAccountID(multiplayer\msgip), BS_EUniverse_Public, BS_EAccountType_Individual)
		BS_ISteamNetworking_CloseP2PSessionWithUser(BS_SteamNetworking(), BS_SteamID_Dynamic)
	endif
	
	for p.players = each players
		multiplayer_removeplayer(p)
	next
	
	multiplayer\IP = 0
	multiplayer\stream = 0
	multiplayer\hosted = False
end function

Function multiplayer_SetMyVariables()
	myplayer\x = EntityX(Collider)
	myplayer\y = EntityY(Collider)
	myplayer\z = EntityZ(Collider)
	myplayer\yaw = EntityYaw(Collider)
	myplayer\pitch = EntityPitch(Camera)
	myplayer\velocity = CurrSpeed+DropSpeed
	myplayer\wearingnightvision = (wearingnightvision<>0)
	myplayer\wearingvest = (wearingvest<>0)
	myplayer\wearinggasmask = (wearinggasmask<>0)
	myplayer\wearinghazmat = (wearinghazmat<>0)
	myplayer\crouchstate = crouchstate
	myplayer\blinktimer = blinktimer
	
	if SelectedItem <> Null Then myplayer\selecteditem = SelectedItem\ID
	if PlayerRoom <> Null Then myplayer\playerroomid = PlayerRoom\ID
End function

Function multiplayer_UpdateRoomObjects()
	if grabbedentity <> 0 then
		For i = 0 To MaxRoomObjects-1
			if GrabbedEntity = PlayerRoom\Objects[i] Then
				if multiplayer_IsAHost() Then 
					PlayerRoom\ObjectUsed[i] = True
				Else
					multiplayer_WriteByte(PACKET_ROTATE)
					multiplayer_WriteByte(myplayer\id)
					multiplayer_WriteByte(i)
					multiplayer_WriteFloat(EntityX(PlayerRoom\Objects[i],True))
					multiplayer_WriteFloat(EntityY(PlayerRoom\Objects[i],True))
					multiplayer_WriteFloat(EntityZ(PlayerRoom\Objects[i],True))
					multiplayer_WriteFloat(EntityPitch(PlayerRoom\Objects[i], True))
					multiplayer_WriteFloat(EntityYaw(PlayerRoom\Objects[i], True))
					multiplayer_WriteFloat(EntityRoll(PlayerRoom\Objects[i], True))
					multiplayer_SendMessage(multiplayer_getip(null), multiplayer_getport(null))
				EndIf
			EndIf
		Next
	endif
End Function

Function multiplayer_PickItem(it.items)
	if multiplayer_IsAHost() Then Return
	multiplayer_WriteByte(PACKET_PICKITEM)
	multiplayer_WriteByte(myplayer\Id)
	multiplayer_WriteShort(it\ID)
	multiplayer_SendMessage(multiplayer_getip(null), multiplayer_getport(null))
end function

Function multiplayer_DropItem(it.items)
	if multiplayer_IsAHost() Then Return
	multiplayer_WriteByte(PACKET_DROPITEM)
	multiplayer_WriteByte(myplayer\Id)
	multiplayer_WriteShort(it\ID)
	multiplayer_SendMessage(multiplayer_getip(null), multiplayer_getport(null))
end function

function multiplayer_SendAnnouncement(file$)
	For p.players = Each players
		if p <> myplayer then
			multiplayer_WriteByte(PACKET_ANNOUNCEMENT)
			multiplayer_WriteLine(file)
			multiplayer_SendMessage(multiplayer_getip(p), multiplayer_getport(p))
		endif
	next
end function

Function multiplayer_CreateNewItem(it.items)
	if multiplayer_IsAHost() Then Return
	multiplayer_WriteByte(PACKET_NEWITEM)
	multiplayer_WriteByte(myplayer\Id)
	multiplayer_WriteShort(it\itemtemplate\templateid)
	multiplayer_SendMessage(multiplayer_getip(null), multiplayer_getport(null))
end function

function multiplayer_RemoveItem(it.items)
	if multiplayer_IsAHost() Then Return
	multiplayer_WriteByte PACKET_REMOVEITEM
	multiplayer_WriteByte(myplayer\Id)
	multiplayer_WriteByte it\ID
	multiplayer_SendMessage(multiplayer_getip(null), multiplayer_getport(null))
end function

Function multiplayer_UseDoor(d.Doors)
	if multiplayer_IsAHost() Then
		For p.players = Each players
			if p <> myplayer then
				multiplayer_WriteByte(PACKET_USEDOOR)
				multiplayer_WriteShort(d\ID)
				multiplayer_WriteByte(d\open)
				multiplayer_WriteByte(d\locked)
				multiplayer_SendMessage(multiplayer_getip(p), multiplayer_getport(p))
			endif
		next
	Else
		multiplayer_WriteByte(PACKET_USEDOOR)
		multiplayer_WriteByte(myplayer\ID)
		multiplayer_WriteShort(d\ID)
		multiplayer_WriteByte(d\open)
		multiplayer_WriteByte(d\locked)
		multiplayer_SendMessage(multiplayer_getip(null), multiplayer_getport(null))
	EndIf
end Function

function multiplayer_SendSingleEvent(e.Events)
	if multiplayer_IsAHost() Then Return
	multiplayer_WriteByte(PACKET_SINGLEEVENT)
	multiplayer_WriteByte(e\ID)
	multiplayer_WriteFloat(e\EventState)
	multiplayer_WriteFloat(e\EventState2)
	multiplayer_WriteFloat(e\EventState3)
	multiplayer_SendMessage(multiplayer_getip(null), multiplayer_getport(null))
end function

Function multiplayer_SendVoice(bnk%)
	if multiplayer_IsAHost() Then
		For p.players = Each players
			if p <> myplayer then
				multiplayer_WriteByte PACKET_VOICE
				multiplayer_WriteByte myplayer\ID
				multiplayer_WriteBytes bnk, 0, BankSize(bnk)
				multiplayer_SendMessage(multiplayer_getip(p), multiplayer_getport(p))
			endif
		next
	Else
		multiplayer_WriteByte PACKET_VOICE
		multiplayer_WriteByte myplayer\ID
		multiplayer_WriteBytes bnk, 0, BankSize(bnk)
		multiplayer_SendMessage(multiplayer_getip(null), multiplayer_getport(null))
	EndIf
End Function

Function multiplayer_SendDecal(de.Decals)
	if multiplayer_IsAHost() Then
		For p.players = Each players
			if p <> myplayer then
				multiplayer_WriteByte PACKET_DECAL
				multiplayer_WriteByte de\ID
				multiplayer_WriteFloat de\x 
				multiplayer_WriteFloat de\y 
				multiplayer_WriteFloat de\z 
				multiplayer_WriteFloat de\yaw 
				multiplayer_WriteFloat de\roll 
				multiplayer_WriteFloat de\pitch 
				multiplayer_WriteFloat de\SizeChange 
				multiplayer_WriteFloat de\Size
				multiplayer_WriteFloat de\MaxSize 
				multiplayer_WriteFloat de\AlphaChange 
				multiplayer_WriteFloat de\Alpha
				multiplayer_WriteFloat de\Timer 
				multiplayer_SendMessage(multiplayer_getip(p), multiplayer_getport(p))
			EndIf
		Next
	Else
		multiplayer_WriteByte PACKET_DECAL
		multiplayer_WriteByte myplayer\ID
		multiplayer_WriteByte de\ID
		multiplayer_WriteFloat de\x 
		multiplayer_WriteFloat de\y 
		multiplayer_WriteFloat de\z 
		multiplayer_WriteFloat de\yaw 
		multiplayer_WriteFloat de\roll 
		multiplayer_WriteFloat de\pitch 
		multiplayer_WriteFloat de\SizeChange 
		multiplayer_WriteFloat de\Size
		multiplayer_WriteFloat de\MaxSize 
		multiplayer_WriteFloat de\AlphaChange 
		multiplayer_WriteFloat de\Alpha
		multiplayer_WriteFloat de\Timer 
		multiplayer_SendMessage(multiplayer_getip(null), multiplayer_getport(null))
	EndIf
End Function

Function multiplayer_SendSound(soundhandle%, distance# = 10.0, volume# = 1.0)
	snd.Sound = Object.Sound(soundhandle)
	if snd <> Null Then
		if multiplayer_IsAHost() Then
			For p.players = Each players
				if p <> myplayer then
					multiplayer_WriteByte PACKET_SOUND
					multiplayer_WriteByte myplayer\ID
					multiplayer_WriteLine snd\name
					multiplayer_WriteFloat distance
					multiplayer_WriteFloat volume
					multiplayer_SendMessage(multiplayer_getip(p), multiplayer_getport(p))
				EndIf
			Next
		Else
			multiplayer_WriteByte PACKET_SOUND
			multiplayer_WriteByte myplayer\ID
			multiplayer_WriteLine snd\name
			multiplayer_WriteFloat distance
			multiplayer_WriteFloat volume
			multiplayer_SendMessage(multiplayer_getip(null), multiplayer_getport(null))
		EndIf
	EndIf
End Function

Function multiplayer_SendTempSound(soundhandle$, X# = 0, Y# = 0, Z# = 0, distance# = 10.0, volume# = 1.0)
	if multiplayer_IsAHost() Then
		For p.players = Each players
			if p <> myplayer then
				multiplayer_WriteByte PACKET_SOUND
				multiplayer_WriteByte myplayer\ID
				multiplayer_WriteLine soundhandle
				multiplayer_WriteFloat distance
				multiplayer_WriteFloat volume
				multiplayer_SendMessage(multiplayer_getip(p), multiplayer_getport(p))
			EndIf
		Next
	Else
		multiplayer_WriteByte PACKET_SOUND
		multiplayer_WriteByte myplayer\ID
		multiplayer_WriteLine soundhandle
		multiplayer_WriteFloat distance
		multiplayer_WriteFloat volume
		multiplayer_SendMessage(multiplayer_getip(null), multiplayer_getport(null))
	EndIf
End Function

Function multiplayer_WriteShort(count)
		PokeShort multiplayer\buffer,multiplayer\bufferwrited, count
		multiplayer\bufferwrited = multiplayer\bufferwrited+2
	End Function
	Function multiplayer_WriteByte(count%)
		PokeByte multiplayer\buffer,multiplayer\bufferwrited, count
		multiplayer\bufferwrited = multiplayer\bufferwrited+1
	End Function
	Function multiplayer_WriteInt(count%)
		PokeInt multiplayer\buffer,multiplayer\bufferwrited, count
		multiplayer\bufferwrited = multiplayer\bufferwrited+4
	End Function
	Function multiplayer_WriteFloat(count#)
		PokeFloat multiplayer\buffer,multiplayer\bufferwrited, count
		multiplayer\bufferwrited = multiplayer\bufferwrited+4
	End Function
	Function multiplayer_WriteLine(msgs$)
		For i = 1 To Len(msgs)
			multiplayer_WriteByte(Asc(Mid(msgs, i, 1)))
		Next
		multiplayer_WriteByte(13)
		multiplayer_WriteByte(10)
	End Function
	Function multiplayer_WriteBytes(buffer%, offset%, size%)
		CopyBank buffer, 0, multiplayer\buffer, multiplayer\bufferwrited, size
		multiplayer\bufferwrited = multiplayer\bufferwrited+size
	End Function
; ============ steam

Function Steam_Update()
	if STEAM_RELEASE Then
		BS_SteamAPI_RunCallbacks()
		;if STEAM_RICH_PRESENCE_UPDATE < MilliSecs() Then
		;	CheckSubscribedItems()
		;	if MainMenuOpen Then
		;		if udp_GetStream() then 
		;			BS_ISteamFriends_SetRichPresence(BS_SteamFriends(), "steam_display", "#Status_WaitingForMatch")
		;			Discord_API_SetState("In lobby")
		;			if Not NetworkServer\SteamStream Then 
		;				BS_ISteamUser_AdvertiseGame(BS_SteamUser(), BS_SteamClient_SteamIDNonSteamGS, udp_network\messIP, udp_network\messPort)
		;				BS_ISteamFriends_SetRichPresence(BS_SteamFriends(), "connect", udp_network\messIP+":"+udp_network\messport)
		;			Else
		;				BS_ISteamUser_AdvertiseGame(BS_SteamUser(), BS_SteamClient_SteamIDNil, 0, 0)
		;				BS_ISteamFriends_SetRichPresence(BS_SteamFriends(), "connect", "")
		;			EndIf
		;		else
		;			BS_ISteamFriends_SetRichPresence(BS_SteamFriends(), "steam_display", "#Status_AtMainMenu")
		;			Discord_API_SetState("In main menu")
		;			BS_ISteamFriends_SetRichPresence(BS_SteamFriends(), "connect", "")
		;			BS_ISteamUser_AdvertiseGame(BS_SteamUser(), BS_SteamClient_SteamIDNil, 0, 0)
		;		endif
		;	Else
		;		if Not udp_GetStream() Then 
		;			BS_ISteamUser_AdvertiseGame(BS_SteamUser(), BS_SteamClient_SteamIDNil, 0, 0)
		;			BS_ISteamFriends_SetRichPresence(BS_SteamFriends(), "connect", "")
		;			BS_ISteamFriends_SetRichPresence(BS_SteamFriends(), "steam_display", "#Status_InGame")
		;			Discord_API_SetState("Playing on a server")
		;		Else
		;			if Not NetworkServer\SteamStream Then 
		;				BS_ISteamUser_AdvertiseGame(BS_SteamUser(), BS_SteamClient_SteamIDNonSteamGS, udp_network\messIP, udp_network\messPort)
		;				BS_ISteamFriends_SetRichPresence(BS_SteamFriends(), "connect", udp_network\messIP+":"+udp_network\messport)
		;			Else
		;				BS_ISteamUser_AdvertiseGame(BS_SteamUser(), BS_SteamClient_SteamIDNil, 0, 0)
		;				BS_ISteamFriends_SetRichPresence(BS_SteamFriends(), "connect", "")
		;			EndIf
		;			BS_ISteamFriends_SetRichPresence(BS_SteamFriends(), "steam_display", "#Status_InGame")
		;			Discord_API_SetState("Playing on a server")
		;		EndIf
		;	EndIf
		;	STEAM_RICH_PRESENCE_UPDATE = MilliSecs()+3000
		;EndIf
	EndIf
End Function

Function OnLobbyCreated(Param%, Param2%, Param3%)
	if LobbyCreated_t = 0 then
		LobbyCreated_t = BP_GetFunctionPointer()
		Return
	EndIf
	if BS_Memory_PeekInt(Param, 0) = BS_EResult_OK then
		;DebugLog BS_Callback_GetCallbackSizeBytes(Session)
		;DebugLog "Result: "+BS_SteamMatchmaking_SetLobbyData(BS_SteamMatchmaking(), Param+4, "steami", Str(BS_CSteamID_GetAccountID(BS_ISteamUser_GetSteamID(BS_SteamUser()))))
		Local BC = BS_CSteamID_FromL(Param+8)
		DebugLog "Setting lobby "+BS_CSteamID_IsLobby(BC)
		DebugLog "Lobby: "+BS_CSteamID_GetAccountID(BC)
		DebugLog "MyID: "+BS_CSteamID_GetAccountID(BS_ISteamUser_GetSteamID(BS_SteamUser()))
		BS_SteamMatchmaking_SetLobbyData(BS_SteamMatchmaking(), BC, "HostSteam", BS_CSteamID_GetAccountID(BS_ISteamUser_GetSteamID(BS_SteamUser())))
	EndIf
	
End Function

Function OnGetFailedSteamConnection(Param%,Param2%,Param3%)
	if P2PSessionRequestFail = 0 then
		P2PSessionRequestFail = BP_GetFunctionPointer()
		Return
	EndIf
	DebugLog "Steam ID: "+BS_Memory_PeekInt(Param, 0)
	DebugLog "Error: "+BS_Memory_PeekByte(Param, 4)

	BS_CSteamID_Set(BS_SteamID_Dynamic, BS_Memory_PeekInt(Param, 0), BS_EUniverse_Public, BS_EAccountType_Individual)

	BS_ISteamNetworking_CloseP2PSessionWithUser(BS_SteamNetworking(), BS_SteamID_Dynamic)
	if multiplayer\CurrentLobby <> 0 And (Not multiplayer\Hosted) Then 
		BS_SteamMatchmaking_LeaveLobby(BS_SteamMatchmaking(), multiplayer\CurrentLobby)
		multiplayer\CurrentLobby = 0
	EndIf
	
End Function

Function OnGetSteamConnection(Param%,Param2%,Param3%)
	if P2PSessionRequest = 0 then
		P2PSessionRequest = BP_GetFunctionPointer()
		Return
	EndIf
	if multiplayer\SteamStream Then
		BS_ISteamNetworking_AcceptP2PSessionWithUser(BS_SteamNetworking(), Param)
		multiplayer_WriteByte STEAM_CONNECT
		multiplayer_WriteByte 2
		multiplayer_SendMessage(Param)
		DebugLog "Sended message to "+Param+" AccountID: "+BS_CSteamID_GetAccountID(Param)
	EndIf
End Function

Function OnJoinedToLobby(Param, Param2%, Param3%)
	if JoinLobbyRequest = 0 then
		JoinLobbyRequest = BP_GetFunctionPointer()
		Return
	EndIf
	;DebugLog "Size: "+BS_Callback_GetCallbackSizeBytes(Session)
	;DebugLog "m_ulSteamIDLobby: "+BS_Memory_PeekLong(Param, 0)
	;DebugLog "m_rgfChatPermissions: "+BS_Memory_PeekInt(Param, 8)
	;;DebugLog "m_bLocked: "+BS_Memory_PeekByte(Param, 12)
	;DebugLog "m_EChatRoomEnterResponse: "+BS_Memory_PeekInt(Param, 13)
	;
	multiplayer\CurrentLobby = BS_CSteamID_FromL(Param)
	if (Not multiplayer\Hosted)Then
		Local SteamID% = BS_SteamMatchmaking_GetLobbyData(BS_SteamMatchmaking(), multiplayer\CurrentLobby, "HostSteam")
		BS_CSteamID_Set(BS_SteamID_Dynamic, SteamID, BS_EUniverse_Public, BS_EAccountType_Individual)
		;BS_ISteamNetworking_AcceptP2PSessionWithUser(BS_SteamNetworking(), BS_SteamID_Dynamic)
		Local Bnk% = CreateBank(1)
		PokeByte Bnk, 0, STEAM_TRYCONNECT
		BS_ISteamNetworking_SendP2PPacket(BS_SteamNetworking(), BS_SteamID_Dynamic, Bnk, 1, 2, 0)
		FreeBank Bnk
		DebugLog "Connected to: "+multiplayer\CurrentLobby
		
		Local WaitingFactor% = MilliSecs()+10000
		
		while waitingfactor > MilliSecs()
			steam_update()
			if steamrecv() <> 0 then
				if multiplayer_ReadByte() = STEAM_CONNECT Then
					multiplayer_connect()
					Exit
				EndIf
			endif
		wend
	EndIf
End Function

Function OnRequestGameLobby(Param%, Param2%, Param3%)
	if GameLobbyRequest = 0 then
		GameLobbyRequest = BP_GetFunctionPointer()
		Return
	EndIf
	if (Not multiplayer\ip) Then
		multiplayer\CurrentLobby = BS_CSteamID_FromL(Param)
		BS_SteamMatchmaking_JoinLobby(BS_SteamMatchmaking(), multiplayer\CurrentLobby)
	EndIf
End Function

; =============

Function multiplayer_ReadFullAvail()
	Return multiplayer_network\availread
End Function
	
Function multiplayer_ReadByte%()
	if multiplayer\bufferreaded+1 > multiplayer_ReadFullAvail() Then Return 0
	Value% = PeekByte(multiplayer\bufferreceive, multiplayer\bufferreaded)
	multiplayer\bufferreaded = multiplayer\bufferreaded + 1
	Return Value%
End Function
Function multiplayer_ReadShort%()
	if multiplayer\bufferreaded+2 > multiplayer_ReadFullAvail() Then Return 0
	Value% = PeekShort(multiplayer\bufferreceive, multiplayer\bufferreaded)
	multiplayer\bufferreaded = multiplayer\bufferreaded + 2
	Return Value%
End Function
Function multiplayer_ReadInt%()
	if multiplayer\bufferreaded+4 > multiplayer_ReadFullAvail() Then Return 0
	Value% = PeekInt(multiplayer\bufferreceive, multiplayer\bufferreaded)
	multiplayer\bufferreaded = multiplayer\bufferreaded + 4
	Return Value%
End Function
Function multiplayer_ReadFloat#()
	if multiplayer\bufferreaded+4 > multiplayer_ReadFullAvail() Then Return 0
	Value# = PeekFloat(multiplayer\bufferreceive, multiplayer\bufferreaded)
	multiplayer\bufferreaded = multiplayer\bufferreaded + 4
	Return Value#
End Function
Function multiplayer_ReadLine$()
	Local ByteValue%
	Local Value$
	While True
		ByteValue = multiplayer_ReadByte()
		if ByteValue = 0 Or ByteValue = 10 Then Exit
		if ByteValue <> 13 Then Value = Value + Chr(ByteValue)
	Wend
	Return Value
End Function
Function multiplayer_ReadBytes(buffer%, offset%, size%)
	local toread% = min(size, multiplayer_ReadAvail())
	CopyBank multiplayer\bufferreceive, multiplayer\bufferreaded, buffer, offset, toread
	multiplayer\bufferreaded = multiplayer\bufferreaded + toread
	Return toRead
End Function

function multiplayer_SendMessage(ip, port = 0)
	BS_ISteamNetworking_SendP2PPacket(BS_SteamNetworking(), IP, multiplayer\buffer, multiplayer\bufferwrited, 2, 0)
	ClearSendBuffer()
end function

Function GetSteamReceive()
	Return multiplayer_network\CSteamID_Buff
End Function
	
function steamrecv()
	Local RecvBank = CreateBank(4), FoundSteam, multiplayer\recv = 0
	if BS_ISteamNetworking_IsP2PPacketAvailable(BS_SteamNetworking(), RecvBank, 0) then
		ClearReadAvail()
		
		multiplayer\recv = GetSteamReceive()
		multiplayer\availread = PeekInt(RecvBank, 0)
		
		if Not BS_ISteamNetworking_ReadP2PPacket(BS_SteamNetworking(), multiplayer\bufferreceive, multiplayer\availread, RecvBank, multiplayer\recv, 0) Then
			BS_CSteamID_Destroy(multiplayer\msgip)
			multiplayer\msgip = 0
			multiplayer\recv = 0
		Else
			if multiplayer\Hosted Then
				FoundSteam = False
				For p.players = Each players
					if p\ID <> multiplayer\MyID Then
						if BS_CSteamID_GetAccountID(p\IP) = BS_CSteamID_GetAccountID(multiplayer\recv) then
							FoundSteam = true
							exit
						endif
					EndIf
				Next
				if Not FoundSteam Then multiplayer\msgip = PutClientSteamID(multiplayer\recv)
			Else
				if multiplayer\msgip = 0 then multiplayer\msgip = PutClientSteamID(multiplayer\recv)
			EndIf
			multiplayer\msgport = 0
		EndIf
	endif
	FreeBank RecvBank
	
	Return multiplayer\recv
end function

Function PutClientSteamID(CSteamI)
	Return BS_CSteamID_Copy(CSteamI)
End Function

Function ClearSendBuffer()
	multiplayer\bufferwrited = 0
End Function

function multiplayer_Send(packet%, IgnoreID% = 0) ; Single send
	if multiplayer_IsAHost() Then
		For p.players = Each players
			if p\ID <> myplayer\ID And p\ID <> ignoreID Then
				multiplayer_WriteByte packet
				multiplayer_SendMessage(multiplayer_getip(p), multiplayer_getport(p))
			EndIf
		Next
	Else
		multiplayer_WriteByte packet
		multiplayer_WriteByte myplayer\ID
		multiplayer_SendMessage(multiplayer_getip(null), multiplayer_getport(null))
	EndIf
	return true
end function
; ====
function multiplayer_getip(p.players)
	if p = null then
		return multiplayer\IP
	else
		return p\IP
	endif
end function
function multiplayer_getport(p.players)
	if p = null then
		return multiplayer\Port
	else
		return p\Port
	endif
end function

function multiplayer_GetStream()
	return multiplayer\stream
end function

function multiplayer_getplayerscount()
	return multiplayer\playerscount
end function

function OnInitNewGame()
	multiplayer_initobjects()
	if multiplayer_IsAHost() then
		p.players = multiplayer_createplayer(1)
		FreeEntity p\BallisticVest
		FreeEntity p\NVGObj
		FreeEntity p\Gasmaskobj
		FreeEntity p\obj
		FreeEntity p\Pivot
		p\Pivot = Collider
		myplayer = p
		myplayer\PlayerBones[1] = Camera
		myplayer\PlayerBones[7] = Camera
		;debuglog "set ent: "+collider
	Else
		myplayer = multiplayer_CreatePlayer(multiplayer\MyID)
		FreeEntity myplayer\BallisticVest
		FreeEntity myplayer\NVGObj
		FreeEntity myplayer\Gasmaskobj
		FreeEntity myplayer\obj
		FreeEntity myplayer\Pivot
		myplayer\Pivot = Collider
		myplayer\PlayerBones[1] = Camera
		myplayer\PlayerBones[7] = Camera
	EndIf
end function

Function OnNullGame()
	multiplayer_close()
End Function

; =========================================================
Global HAZMAT_OBJECT
Global NVG_OBJ
Global VEST_OBJ
Global GASMASK_OBJ
function multiplayer_SetPlayerTexture(Seed%, Entity%)
	PreviousSeed% = RndSeed()
	SeedRnd Seed
	
	Select Rand(0, 2)
		Case 0
			Texture% = LoadTexture_Strict("GFX\npcs\class_d.png")
		Case 1
			Texture% = LoadTexture_Strict("GFX\npcs\class_d(2).png")
		Case 2
			Texture% = LoadTexture_Strict("GFX\npcs\d_9341.png")
	End Select
	
	if Texture <> 0 Then
		EntityTexture Entity, Texture%
		FreeTexture Texture
	EndIf
	
	SeedRnd PreviousSeed
End Function
function multiplayer_createplayer.players(id = 0)
	;Local o.Objects = First Objects
	local p.players
	if id = 0 then
		p = new players
		p\id = findfreeplayerid()
	else
		for p2.players = each players
			if p2\ID = id then return p2
		next
		p = new players
		p\id = id
	endif
	p\obj = copyentity(o\NPCModelID[3])
	scaleentity(p\obj, 0.5 / MeshWidth(p\obj), 0.5 / MeshWidth(p\obj), 0.5 / MeshWidth(p\obj))
	p\Pivot = CreatePivot()
	EntityType p\Pivot, HIT_PLAYER
	EntityRadius p\Pivot, 0.1, 1
	EntityPickMode p\Pivot, 1
		
	NameEntity(p\obj, "Body")
	
	tex = LoadTexture_Strict("GFX\items\Vest.png")

	p\BallisticVest = CopyEntity(VEST_OBJ)
	EntityTexture p\BallisticVest, tex
	ScaleEntity(p\BallisticVest, 0.022,0.045,0.022)
	p\GasMaskObj = CopyEntity(GASMASK_OBJ)
	ScaleEntity(p\GasMaskObj, 0.02,0.02,0.02)
	p\NVGobj = CopyEntity(NVG_OBJ)
	ScaleEntity(p\NVGobj, 0.02,0.02,0.02)
	FreeTexture tex
	
	p\VoiceBank = CreateBank(0)
	
	multiplayer_SetPlayerTexture(p\ID, p\obj)
	multiplayer_ResetPlayerBonesValues(p)
	return p
end function

function multiplayer_updateplayer(p.players)
	if p\ID <> multiplayer\MyID Then
		
		p\isdead = (p\PLAYER_MOVE=PLAYER_DEAD)
		
		select lower(entityname(p\obj))
			case "body"
				if p\wearinghazmat = true then
					freeentity p\obj
					p\obj = copyentity(HAZMAT_OBJECT)
					scaleentity p\obj, 0.013, 0.013, 0.013
					nameentity p\obj, "hazmat"
					
					multiplayer_ResetPlayerBonesValues(p)
				endif
			case "hazmat"
				if p\wearinghazmat = false then
					;Local o.Objects = First Objects
					freeentity p\obj
					p\obj = copyentity(o\NPCModelID[3])
					scaleentity p\obj, 0.5 / MeshWidth(p\obj), 0.5 / MeshWidth(p\obj), 0.5 / MeshWidth(p\obj)
					nameentity p\obj, "body"
					
					multiplayer_SetPlayerTexture(p\ID, p\obj)
					
					multiplayer_ResetPlayerBonesValues(p)
				endif
		end select
		
		Local FixPivot# = 0.32, FixRotate# = 180, FixPitch = 0
		
		if Distance(p\x, p\z, EntityX(p\Pivot), EntityZ(p\Pivot)) > 8 Then
			PositionEntity(p\Pivot, p\x#, p\y#, p\z#)
			ResetEntity p\Pivot
		EndIf
		;DebugLog "p\y = "+p\y
		
		PrevX# = EntityX(p\Pivot)
		PrevZ# = EntityZ(p\Pivot)
		
		speedx# = curvevalue(p\x, EntityX(p\Pivot), 5)
		speedy# = curvevalue(p\y, EntityY(p\Pivot), 5)
		speedz# = curvevalue(p\z, EntityZ(p\Pivot), 5)
		speedyaw# = curveangle(p\legyaw, EntityYaw(p\Pivot), 7)
		
		PositionEntity p\Pivot, speedx, speedy, speedz
		RotateEntity(p\Pivot,0,speedyaw,0)
		ResetEntity p\Pivot
		
		if Distance(PrevX, PrevZ, EntityX(p\Pivot), EntityZ(p\Pivot)) >= 0.02 Then ; Set the correct animation if we have a high ping
			Select p\PLAYER_MOVE
				Case PLAYER_IDLING
					p\PLAYER_MOVE = PLAYER_WALKING
				Case PLAYER_SITTING_IDLING
					p\PLAYER_MOVE = PLAYER_SITTING_WALKING_FORWARD
			End Select
		;Else
		;	Select p\PLAYER_MOVE
		;		Case PLAYER_WALKING
		;			p\PLAYER_MOVE = PLAYER_IDLING
		;		Case PLAYER_SITTING_WALKING_BACK,PLAYER_SITTING_WALKING_FORWARD,PLAYER_SITTING_WALKING_LEFT,PLAYER_SITTING_WALKING_RIGHT
		;			p\PLAYER_MOVE = PLAYER_SITTING_IDLING
		;	End Select
		EndIf
		;Else
		;	Select p\PLAYER_MOVE
		;		Case PLAYER_RUNNING,PLAYER_WALKING
		;			MoveEntity p\Pivot, 0.0, 0.0, 0.015*fs\FPSfactor[0]
		;			p\x = EntityX(p\Pivot)
		;			p\y = EntityY(p\Pivot)
		;			p\z = EntityZ(p\Pivot)
		;			
		;		Case PLAYER_SITTING_WALKING_BACK,PLAYER_SITTING_WALKING_FORWARD,PLAYER_SITTING_WALKING_LEFT,PLAYER_SITTING_WALKING_RIGHT
		;			MoveEntity p\Pivot, 0.0, 0.0, 0.0075*fs\FPSfactor[0]
		;			p\x = EntityX(p\Pivot)
		;			p\y = EntityY(p\Pivot)
		;			p\z = EntityZ(p\Pivot)
		;		Case PLAYER_RUNNING
		;			p\PLAYER_MOVE = PLAYER_WALKING
		;			MoveEntity p\Pivot, 0.0, 0.0, 0.015*fs\FPSfactor[0]
		;			
		;			p\x = EntityX(p\Pivot)
		;			p\y = EntityY(p\Pivot)
		;			p\z = EntityZ(p\Pivot)
		;	End Select
		;EndIf
		
		multiplayer_updateanimationsplayer(p)
		
		
		PositionEntity(p\obj, EntityX(p\Pivot), EntityY(p\Pivot) - FixPivot, EntityZ(p\Pivot))
		if p\PlayerBones[1] = 0 Then p\PlayerBones[1] = FindChild(p\obj,"Bip01_Head")
		PositionEntity(p\PlayerBones[7], EntityX(p\PlayerBones[1], True),EntityY(p\PlayerBones[1], True),EntityZ(p\PlayerBones[1], True))
		RotateEntity(p\PlayerBones[7], 0, p\yaw, 0)
		
		if not p\isdead then RotateEntity(p\obj,FixPitch,WrapAngle(EntityYaw(p\Pivot)), 0, True)
		
		if p\PlayerBones[0] = 0 then p\PlayerBones[0] = FindChild(p\obj, "Bip01_Spine1")
		
		RotateEntity p\PlayerBones[0], p\pitch, p\yaw, EntityRoll(p\PlayerBones[0], True), True
		
		
		multiplayer_updateattachplayer(p)
	endif
end function

Function multiplayer_ResetPlayerBonesValues(p.Players)
	For i = 0 To 8
		p\PlayerBones[i] = 0
	Next

	if p\PlayerBones[7] = 0 Then 
		p\PlayerBones[7] = CreateCamera()
		CameraProjMode p\PlayerBones[7], 0
	EndIf
End Function

function multiplayer_updateattachplayer(p.players)
	; other attachs
	if EntityName(p\obj) = "Body" then
		if p\PlayerBones[1] = 0 Then p\PlayerBones[1] = FindChild(p\obj,"Bip01_Head")

		q# = EntityX(p\PlayerBones[1], True)
		w# = EntityZ(p\PlayerBones[1], True)
		q# =  q + (0.025 * Sin(-p\yaw))
		w# =  w + (0.025 * Cos(-p\yaw))

		AttachObject(p\GasMaskObj, q,EntityY(p\PlayerBones[1], True)+0.01,w, EntityPitch(p\PlayerBones[1], True)+90,EntityYaw(p\PlayerBones[1], True),0, -1, p\wearinggasmask)
		
		AttachObject(p\NVGObj, q,EntityY(p\PlayerBones[1], True)+0.05,w, EntityPitch(p\PlayerBones[1], True),EntityYaw(p\PlayerBones[1], True),EntityRoll(p\PlayerBones[1]), -1, p\wearingnightvision)
		
		if p\PlayerBones[2] = 0 then p\PlayerBones[2] = FindChild(p\obj,"Bip01_Spine2")
		q = EntityX(p\PlayerBones[2], True)
		w = EntityZ(p\PlayerBones[2], True)
		q# =  q + (0.1 * Sin(-p\yaw))
		w# =  w + (0.1 * Cos(-p\yaw))
		
		AttachObject(p\BallisticVest, q,EntityY(p\PlayerBones[2], True)-0.08,w, EntityPitch(p\PlayerBones[2], True)+90,EntityYaw(p\PlayerBones[2], True)-180,EntityRoll(p\PlayerBones[2], True)+90, -1, p\WearingVest)
		
		if p\SelectedItem <> 0 Then
			i.Items = Item[p\SelectedItem]
			if i <> Null Then
				if p\PlayerBones[3] = 0 then p\PlayerBones[3] = FindChild(p\obj,"Bip01_R_Finger0")
				if p\PlayerBones[3] <> 0 Then
					if EntityDistance(Camera, p\PlayerBones[3]) < HideDist Then
						ShowEntity i\collider
						PositionEntity i\collider, EntityX(p\PlayerBones[3], True), EntityY(p\PlayerBones[3], True), EntityZ(p\PlayerBones[3], True), True
						RotateEntity i\collider, EntityPitch(p\PlayerBones[3], True)-45, EntityYaw(p\PlayerBones[3], True)-70, EntityRoll(p\PlayerBones[3], True)+20, True
						ResetEntity i\collider
					EndIf
				EndIf
			EndIf
		EndIf
	Else
		HideEntity p\BallisticVest
		HideEntity p\NVGObj
		HideEntity p\Gasmaskobj
	EndIf
end function

Function AttachObject(objectid%, xbone#, ybone#, zbone#, pitchbone#, yaw#, roll#, startint, endint)
	
	if startint = -1 Then
		if endint > 0 Then
			PositionEntity(objectid, xbone,ybone,zbone, True)
			RotateEntity objectid,pitchbone,yaw,roll, True
			ShowEntity objectid
			Return True
		Else
			HideEntity objectid
		Endif
	Else
		if startint = endint Then
			PositionEntity(objectid, xbone,ybone,zbone, True)
			RotateEntity objectid,pitchbone,yaw,roll, True
			ShowEntity objectid
			Return True
		Else
			HideEntity objectid
		EndIf
	EndIf
	Return False
End Function

function multiplayer_updateanimationsplayer(p.players)
	If p\PLAYER_MOVE = PLAYER_SITTING_IDLING Then 
		Animate2(p\obj, AnimTime(p\obj), 357, 381, 0.1)
	Elseif p\PLAYER_MOVE = PLAYER_IDLING Then
		Animate2(p\obj, AnimTime(p\obj), 210, 235, 0.1)
	Elseif p\PLAYER_MOVE = PLAYER_RUNNING Then
		Animate2(p\obj, AnimTime(p\obj), 301, 319, 0.3) 
	Elseif p\PLAYER_MOVE = PLAYER_WALKING Then
		Animate2(p\obj, AnimTime(p\obj), 236, 260, 0.3) 
	Elseif p\PLAYER_MOVE = PLAYER_SITTING_WALKING_FORWARD Then
		Animate2(p\obj, AnimTime(p\obj), 382, 406, 0.3)
	Elseif p\PLAYER_MOVE = PLAYER_SITTING_WALKING_BACK Then
		Animate2(p\obj, AnimTime(p\obj), 382, 406, -0.3)
	Elseif p\PLAYER_MOVE = PLAYER_JUMPING Then
		Animate2(p\obj, AnimTime(p\obj), 834, 894, 1) 
	Elseif p\PLAYER_MOVE = PLAYER_SITTING_WALKING_LEFT Then
		Animate2(p\obj, AnimTime(p\obj), 261,280, 0.3)
	Elseif p\PLAYER_MOVE = PLAYER_SITTING_WALKING_RIGHT Then
		Animate2(p\obj, AnimTime(p\obj), 281, 300, 0.3)
	Elseif p\PLAYER_MOVE = PLAYER_DEAD Then
		Animate2(p\obj, AnimTime(p\obj), 0, 19, 0.3, False)
	EndIf
end function

function multiplayer_removeplayer(p.players)
	FreeEntity p\BallisticVest
	FreeEntity p\NVGObj
	FreeEntity p\Gasmaskobj
	FreeEntity p\obj
	FreeEntity p\Pivot
	FreeBank p\VoiceBank
	
	if multiplayer\hosted then
		BS_CSteamID_Set(BS_SteamID_Dynamic, BS_CSteamID_GetAccountID(p\IP), BS_EUniverse_Public, BS_EAccountType_Individual)
		BS_ISteamNetworking_CloseP2PSessionWithUser(BS_SteamNetworking(), BS_SteamID_Dynamic)
		BS_CSteamID_Destroy(p\IP)
	endif
	
	Delete p
end function

Function ReadBool(byte, index)
	Return ((byte Shr index) Mod 2)
End Function

function multiplayer_initobjects()
	HAZMAT_OBJECT = LoadAnimMesh(NPCsPath$+"hazmat.b3d")
	NVG_OBJ = LoadMesh(ItemsPath$+"night_vision_goggles.b3d")
	GASMASK_OBJ = LoadMesh(ItemsPath$+"gas_mask.b3d")
	VEST_OBJ = LoadMesh(ItemsPath$+"vest.x")
	HideEntity HAZMAT_OBJECT
	HideEntity NVG_OBJ
	HideEntity GASMASK_OBJ
	HideEntity VEST_OBJ
end function


Function IsABlockedRoom%(r.Rooms)
	Select Lower(r\RoomTemplate\name)
		Case "dimension1499"
			Return True
		Case "pocketdimension"
			Return True
	End Select
	Return False
End Function






; /////////////////////////////////////
Function flipvalue#(value#, maxvalue#)
	if value >= max_value then value = 0
	return value
End Function

Const MAX_ITEMS = 255
Const MAX_DOORS = 500
Const MAX_EVENTS = 100
Const MAX_ROOMS = 255
Const MAX_NPCS = 255

Global Room.Rooms[MAX_ROOMS]
Global Item.Items[MAX_ITEMS]
Global NPC.NPCs[MAX_NPCS]
Global Door.Doors[MAX_DOORS]
Global Event.Events[MAX_EVENTS]

;function redefinedoors()
;	for d.doors = each doors
;		d\ID = FindFreeItemID()
;;		Item[d\ID] = d
;	next
;end function
;function redefinerooms()
;	for r.Rooms = each Rooms
;		r\ID = FindFreeRoomID()
;		Room[r\ID] = r
;	next
;end function
;function redefineitems()
;	for it.items = each items
;		it\ID = FindFreeItemID()
;		Item[it\ID] = it
;	next
;end function

function OnEndingLoad()
	;redefinedoors
	;redefinerooms
	;redefineitems
	;redefineevents
	;redefinenpcs
end function

Function SetItemID(it.Items, ID)
	Item[ID] = it
	it\ID = ID
End Function
Function SetNPCID(n.NPCs, ID)
	NPC[ID] = n
	n\ID = ID
End Function

Function findfreeroomid%(r.rooms)
	for i = 1 to MAX_ROOMS-1
		if Room[i] = null then
			Room[i] = r
			return i
		endif
	next
end function
Function findfreeitemid%(it.items)
	for i = 1 to MAX_ITEMS-1
		if Item[i] = null then
			Item[i] = it
			return i
		endif
	next
end function
Function findfreenpcid%(n.npcs)
	for i = 1 to MAX_NPCS-1
		if NPC[i] = null then
			NPC[i] = n
			return i
		EndIf
	next
end function
Function findfreedoorid%(d.doors)
	for i = 1 to MAX_DOORS-1
		if Door[i] = null then
			Door[i] = d
			return i
		EndIf
	next
end function
Function findfreeeventid%(e.events)
	for i = 1 to MAX_EVENTS-1
		if Event[i] = null then
			Event[i] = e
			return i
		EndIf
	next
end function

function findfreeplayerid()
	Local id% = 1
	While (True)
		Local taken% = False
		For p.players = Each players
			If p\ID = id Then
				taken = True
				Exit
			EndIf
		Next
		If (Not taken) Then
			Return id
		EndIf
		id = id + 1
	Wend
end function

Function CalculateDist#(obj, r.Rooms)
	Return Max(Abs(r\x-EntityX(obj,True)),Abs(r\z-EntityZ(obj,True)))
End Function

Function IsANotRemovedEvent%(e.Events)
	Select e\eventconst
		Case e_room1162,e_room1123
			Return True
		Case e_gateb
			Return True
		Case e_Gatea
			Return True
		Case e_room3storage
			Return True
		Case e_pocketdimension
			Return True
		Case e_room860
			Return True
		Case e_dimension1499
			Return True
		Case e_room914
			Return True
		Case e_room2tesla
			Return True
		Case e_room2tunnel
			Return true
		Case E_ROOM173INTRO
			Return True
	End Select
	Return False
End Function