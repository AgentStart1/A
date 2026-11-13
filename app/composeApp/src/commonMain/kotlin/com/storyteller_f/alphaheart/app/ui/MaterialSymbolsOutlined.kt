/*
 * This is a private project. All rights reserved.
 */

@file:Suppress(
    "UnusedReceiverParameter",
    "Unused",
    "ObjectPropertyName",
    "SpellCheckingInspection",
    "ConstPropertyName",
)

package com.storyteller_f.alphaheart.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.material_symbols_outlined
import dev.tclement.fonticons.ExperimentalFontIconsApi
import dev.tclement.fonticons.VariableIconFont
import dev.tclement.fonticons.rememberVariableIconFont

@Suppress("ObjectPropertyNaming", "LargeClass")
object MaterialSymbolsOutlined {
    const val version: String = "2.791"

    @Composable
    // FontResource loading uses an in-memory descriptor on our Android 33+ targets.
    @OptIn(ExperimentalFontIconsApi::class)
    fun rememberIconFont(fill: Boolean = false, grade: Int = 0, fontFeatureSettings: String? = null): VariableIconFont =
        rememberVariableIconFont(
            fontResource = Res.font.material_symbols_outlined,
            weights =
            arrayOf(
                FontWeight.Thin,
                FontWeight.ExtraLight,
                FontWeight.Light,
                FontWeight.Normal,
                FontWeight.Medium,
                FontWeight.SemiBold,
                FontWeight.Bold,
                FontWeight.ExtraBold,
                FontWeight.Black,
            ),
            fontVariationSettings =
            FontVariation.Settings(
                FontVariation.Setting("FILL", if (fill) 1f else 0f),
                FontVariation.Setting("GRAD", grade.toFloat()),
            ),
            fontFeatureSettings = fontFeatureSettings,
        )

    const val Space: Char = '\u0020'

    const val Period: Char = '\u002e'

    const val Underscore: Char = '\u005f'

    const val Error: Char = '\ue000'

    const val Warning: Char = '\ue002'

    const val AddAlert: Char = '\ue003'

    const val NotificationImportant: Char = '\ue004'

    const val QrCode2: Char = '\ue00a'

    const val FlutterDash: Char = '\ue00b'

    const val AlignVerticalTop: Char = '\ue00c'

    const val AlignHorizontalLeft: Char = '\ue00d'

    const val AlignHorizontalCenter: Char = '\ue00f'

    const val AlignHorizontalRight: Char = '\ue010'

    const val AlignVerticalCenter: Char = '\ue011'

    const val TeamDashboard: Char = '\ue013'

    const val HorizontalDistribute: Char = '\ue014'

    const val AlignVerticalBottom: Char = '\ue015'

    const val BringYourOwnIp: Char = '\ue016'

    const val KeepOff: Char = '\ue017'

    const val DiscoverTune: Char = '\ue018'

    const val Album: Char = '\ue019'

    const val Artist: Char = '\ue01a'

    const val AvTimer: Char = '\ue01b'

    const val ClosedCaption: Char = '\ue01c'

    const val Equalizer: Char = '\ue01d'

    const val Explicit: Char = '\ue01e'

    const val FastForward: Char = '\ue01f'

    const val FastRewind: Char = '\ue020'

    const val Gamepad: Char = '\ue021'

    const val Genres: Char = '\ue022'

    const val Hearing: Char = '\ue023'

    const val HighQuality: Char = '\ue024'

    const val Ifl: Char = '\ue025'

    const val InstantMix: Char = '\ue026'

    const val Ios: Char = '\ue027'

    const val Autorenew: Char = '\ue028'

    const val Mic: Char = '\ue029'

    const val MicOff: Char = '\ue02b'

    const val Movie: Char = '\ue02c'

    const val MovieInfo: Char = '\ue02d'

    const val LibraryAdd: Char = '\ue02e'

    const val LibraryBooks: Char = '\ue02f'

    const val LibraryMusic: Char = '\ue030'

    const val Verified: Char = '\ue031'

    const val News: Char = '\ue032'

    const val Block: Char = '\ue033'

    const val Pause: Char = '\ue034'

    const val PauseCircle: Char = '\ue035'

    const val PlayArrow: Char = '\ue037'

    const val PlayCircle: Char = '\ue038'

    const val PlaylistAdd: Char = '\ue03b'

    const val QueueMusic: Char = '\ue03d'

    const val Radio: Char = '\ue03e'

    const val RecentActors: Char = '\ue03f'

    const val Repeat: Char = '\ue040'

    const val RepeatOne: Char = '\ue041'

    const val Replay: Char = '\ue042'

    const val Shuffle: Char = '\ue043'

    const val SkipNext: Char = '\ue044'

    const val SkipPrevious: Char = '\ue045'

    const val Snooze: Char = '\ue046'

    const val Stop: Char = '\ue047'

    const val Subtitles: Char = '\ue048'

    const val SurroundSound: Char = '\ue049'

    const val VideoLibrary: Char = '\ue04a'

    const val Videocam: Char = '\ue04b'

    const val VideocamOff: Char = '\ue04c'

    const val VolumeDown: Char = '\ue04d'

    const val VolumeMute: Char = '\ue04e'

    const val VolumeOff: Char = '\ue04f'

    const val VolumeUp: Char = '\ue050'

    const val Web: Char = '\ue051'

    const val Hd: Char = '\ue052'

    const val SortByAlpha: Char = '\ue053'

    const val Airplay: Char = '\ue055'

    const val Forward10: Char = '\ue056'

    const val Forward30: Char = '\ue057'

    const val Forward5: Char = '\ue058'

    const val Replay10: Char = '\ue059'

    const val Replay30: Char = '\ue05a'

    const val Replay5: Char = '\ue05b'

    const val AddToQueue: Char = '\ue05c'

    const val FiberDvr: Char = '\ue05d'

    const val FiberNew: Char = '\ue05e'

    const val PlaylistPlay: Char = '\ue05f'

    const val ArtTrack: Char = '\ue060'

    const val FiberManualRecord: Char = '\ue061'

    const val FiberSmartRecord: Char = '\ue062'

    const val MusicVideo: Char = '\ue063'

    const val Subscriptions: Char = '\ue064'

    const val PlaylistAddCheck: Char = '\ue065'

    const val QueuePlayNext: Char = '\ue066'

    const val RemoveFromQueue: Char = '\ue067'

    const val SlowMotionVideo: Char = '\ue068'

    const val WebAsset: Char = '\ue069'

    const val FiberPin: Char = '\ue06a'

    const val BrandingWatermark: Char = '\ue06b'

    const val CallToAction: Char = '\ue06c'

    const val FeaturedPlayList: Char = '\ue06d'

    const val FeaturedVideo: Char = '\ue06e'

    const val Note: Char = '\ue06f'

    const val VideoCall: Char = '\ue070'

    const val VideoLabel: Char = '\ue071'

    const val _4k: Char = '\ue072'

    const val MissedVideoCall: Char = '\ue073'

    const val ControlCamera: Char = '\ue074'

    const val UpdateDisabled: Char = '\ue075'

    const val VerticalDistribute: Char = '\ue076'

    const val Start: Char = '\ue089'

    const val CallLog: Char = '\ue08e'

    const val AddNotes: Char = '\ue091'

    const val AllMatch: Char = '\ue093'

    const val Allergies: Char = '\ue094'

    const val BloodPressure: Char = '\ue097'

    const val BodyFat: Char = '\ue098'

    const val BodySystem: Char = '\ue099'

    const val Cardiology: Char = '\ue09c'

    const val ClinicalNotes: Char = '\ue09e'

    const val Cognition: Char = '\ue09f'

    const val Conditions: Char = '\ue0a0'

    const val Congenital: Char = '\ue0a1'

    const val Deceased: Char = '\ue0a5'

    const val Dentistry: Char = '\ue0a6'

    const val Dermatology: Char = '\ue0a7'

    const val Diagnosis: Char = '\ue0a8'

    const val Endocrinology: Char = '\ue0a9'

    const val Ent: Char = '\ue0aa'

    const val ExportNotes: Char = '\ue0ac'

    const val FamilyHistory: Char = '\ue0ad'

    const val Flowsheet: Char = '\ue0ae'

    const val Domain: Char = '\ue0af'

    const val Call: Char = '\ue0b0'

    const val CallEnd: Char = '\ue0b1'

    const val CallMade: Char = '\ue0b2'

    const val CallMerge: Char = '\ue0b3'

    const val CallMissed: Char = '\ue0b4'

    const val CallReceived: Char = '\ue0b5'

    const val CallSplit: Char = '\ue0b6'

    const val Chat: Char = '\ue0b7'

    const val ClearAll: Char = '\ue0b8'

    const val Comment: Char = '\ue0b9'

    const val Contacts: Char = '\ue0ba'

    const val DialerSip: Char = '\ue0bb'

    const val Dialpad: Char = '\ue0bc'

    const val Mail: Char = '\ue0be'

    const val Forum: Char = '\ue0bf'

    const val HangoutVideo: Char = '\ue0c1'

    const val HangoutVideoOff: Char = '\ue0c2'

    const val SwapVert: Char = '\ue0c3'

    const val InvertColorsOff: Char = '\ue0c4'

    const val LiveHelp: Char = '\ue0c6'

    const val LocationOff: Char = '\ue0c7'

    const val Place: Char = '\ue0c8'

    const val ChatBubble: Char = '\ue0ca'

    const val NoSim: Char = '\ue0cc'

    const val WifiTetheringOff: Char = '\ue0ce'

    const val ContactPhone: Char = '\ue0cf'

    const val ContactMail: Char = '\ue0d0'

    const val RingVolume: Char = '\ue0d1'

    const val SpeakerPhone: Char = '\ue0d2'

    const val StayCurrentLandscape: Char = '\ue0d3'

    const val StayCurrentPortrait: Char = '\ue0d4'

    const val StayPrimaryLandscape: Char = '\ue0d5'

    const val StayPrimaryPortrait: Char = '\ue0d6'

    const val SwapCalls: Char = '\ue0d7'

    const val Sms: Char = '\ue0d8'

    const val Voicemail: Char = '\ue0d9'

    const val VpnKey: Char = '\ue0da'

    const val PhonelinkErase: Char = '\ue0db'

    const val PhonelinkLock: Char = '\ue0dc'

    const val PhonelinkRing: Char = '\ue0dd'

    const val PhonelinkSetup: Char = '\ue0de'

    const val PresentToAll: Char = '\ue0df'

    const val ImportContacts: Char = '\ue0e0'

    const val ScreenShare: Char = '\ue0e2'

    const val StopScreenShare: Char = '\ue0e3'

    const val CallMissedOutgoing: Char = '\ue0e4'

    const val RssFeed: Char = '\ue0e5'

    const val AlternateEmail: Char = '\ue0e6'

    const val MobileScreenShare: Char = '\ue0e7'

    const val AddCall: Char = '\ue0e8'

    const val CancelPresentation: Char = '\ue0e9'

    const val PausePresentation: Char = '\ue0ea'

    const val Unsubscribe: Char = '\ue0eb'

    const val CellWifi: Char = '\ue0ec'

    const val SentimentSatisfied: Char = '\ue0ed'

    const val ListAlt: Char = '\ue0ee'

    const val DomainDisabled: Char = '\ue0ef'

    const val Lightbulb: Char = '\ue0f0'

    const val Gastroenterology: Char = '\ue0f1'

    const val Genetics: Char = '\ue0f3'

    const val Gynecology: Char = '\ue0f4'

    const val Hematology: Char = '\ue0f6'

    const val Immunology: Char = '\ue0fb'

    const val InactiveOrder: Char = '\ue0fc'

    const val Inpatient: Char = '\ue0fe'

    const val LabPanel: Char = '\ue103'

    const val LabProfile: Char = '\ue104'

    const val Labs: Char = '\ue105'

    const val Lda: Char = '\ue106'

    const val Metabolism: Char = '\ue10b'

    const val Microbiology: Char = '\ue10c'

    const val Nephrology: Char = '\ue10d'

    const val Neurology: Char = '\ue10e'

    const val Nutrition: Char = '\ue110'

    const val Oncology: Char = '\ue114'

    const val Ophthalmology: Char = '\ue115'

    const val OralDisease: Char = '\ue116'

    const val Outpatient: Char = '\ue118'

    const val OutpatientMed: Char = '\ue119'

    const val Pediatrics: Char = '\ue11d'

    const val PhysicalTherapy: Char = '\ue11e'

    const val Pill: Char = '\ue11f'

    const val Podiatry: Char = '\ue120'

    const val Prescriptions: Char = '\ue121'

    const val Problem: Char = '\ue122'

    const val Psychiatry: Char = '\ue123'

    const val Pulmonology: Char = '\ue124'

    const val Radiology: Char = '\ue125'

    const val RespiratoryRate: Char = '\ue127'

    const val Rheumatology: Char = '\ue128'

    const val SourceNotes: Char = '\ue12d'

    const val Surgical: Char = '\ue131'

    const val Symptoms: Char = '\ue132'

    const val Syringe: Char = '\ue133'

    const val Urology: Char = '\ue137'

    const val Vaccines: Char = '\ue138'

    const val Ventilator: Char = '\ue139'

    const val Vitals: Char = '\ue13b'

    const val Ward: Char = '\ue13c'

    const val Weight: Char = '\ue13d'

    const val Woman: Char = '\ue13e'

    const val WoundsInjuries: Char = '\ue13f'

    const val Add: Char = '\ue145'

    const val AddBox: Char = '\ue146'

    const val AddCircle: Char = '\ue147'

    const val Archive: Char = '\ue149'

    const val Backspace: Char = '\ue14a'

    const val Close: Char = '\ue14c'

    const val ContentCopy: Char = '\ue14d'

    const val ContentCut: Char = '\ue14e'

    const val ContentPaste: Char = '\ue14f'

    const val Edit: Char = '\ue150'

    const val Drafts: Char = '\ue151'

    const val FilterList: Char = '\ue152'

    const val Flag: Char = '\ue153'

    const val Forward: Char = '\ue154'

    const val Gesture: Char = '\ue155'

    const val Inbox: Char = '\ue156'

    const val Link: Char = '\ue157'

    const val Redo: Char = '\ue15a'

    const val Remove: Char = '\ue15b'

    const val DoNotDisturbOn: Char = '\ue15c'

    const val Reply: Char = '\ue15e'

    const val ReplyAll: Char = '\ue15f'

    const val Report: Char = '\ue160'

    const val Save: Char = '\ue161'

    const val SelectAll: Char = '\ue162'

    const val Send: Char = '\ue163'

    const val Sort: Char = '\ue164'

    const val TextFormat: Char = '\ue165'

    const val Undo: Char = '\ue166'

    const val FontDownload: Char = '\ue167'

    const val MoveToInbox: Char = '\ue168'

    const val Unarchive: Char = '\ue169'

    const val NextWeek: Char = '\ue16a'

    const val Weekend: Char = '\ue16b'

    const val DeleteSweep: Char = '\ue16c'

    const val LowPriority: Char = '\ue16d'

    const val LinkOff: Char = '\ue16f'

    const val ReportOff: Char = '\ue170'

    const val Download: Char = '\ue171'

    const val Ballot: Char = '\ue172'

    const val FileCopy: Char = '\ue173'

    const val HowToReg: Char = '\ue174'

    const val HowToVote: Char = '\ue175'

    const val Waves: Char = '\ue176'

    const val WhereToVote: Char = '\ue177'

    const val AddLink: Char = '\ue178'

    const val Inventory: Char = '\ue179'

    const val Mist: Char = '\ue188'

    const val Alarm: Char = '\ue190'

    const val Schedule: Char = '\ue192'

    const val AlarmAdd: Char = '\ue193'

    const val AirplanemodeInactive: Char = '\ue194'

    const val AirplanemodeActive: Char = '\ue195'

    const val Tornado: Char = '\ue199'

    const val BatteryAlert: Char = '\ue19c'

    const val ShopTwo: Char = '\ue19e'

    const val Priority: Char = '\ue19f'

    const val Workspaces: Char = '\ue1a0'

    const val Inventory2: Char = '\ue1a1'

    const val BatteryChargingFull: Char = '\ue1a3'

    const val BatteryFull: Char = '\ue1a4'

    const val BatteryUnknown: Char = '\ue1a6'

    const val Bluetooth: Char = '\ue1a7'

    const val BluetoothConnected: Char = '\ue1a8'

    const val BluetoothDisabled: Char = '\ue1a9'

    const val BluetoothSearching: Char = '\ue1aa'

    const val BrightnessAuto: Char = '\ue1ab'

    const val BrightnessHigh: Char = '\ue1ac'

    const val BrightnessLow: Char = '\ue1ad'

    const val BrightnessMedium: Char = '\ue1ae'

    const val DataUsage: Char = '\ue1af'

    const val DeveloperMode: Char = '\ue1b0'

    const val Devices: Char = '\ue1b1'

    const val Dvr: Char = '\ue1b2'

    const val MyLocation: Char = '\ue1b3'

    const val LocationSearching: Char = '\ue1b4'

    const val LocationDisabled: Char = '\ue1b5'

    const val GraphicEq: Char = '\ue1b8'

    const val NetworkCell: Char = '\ue1b9'

    const val NetworkWifi: Char = '\ue1ba'

    const val Nfc: Char = '\ue1bb'

    const val Wallpaper: Char = '\ue1bc'

    const val Widgets: Char = '\ue1bd'

    const val ScreenLockLandscape: Char = '\ue1be'

    const val ScreenLockPortrait: Char = '\ue1bf'

    const val ScreenLockRotation: Char = '\ue1c0'

    const val ScreenRotation: Char = '\ue1c1'

    const val SdCard: Char = '\ue1c2'

    const val SettingsSystemDaydream: Char = '\ue1c3'

    const val EditLocationAlt: Char = '\ue1c5'

    const val WbTwilight: Char = '\ue1c6'

    const val SignalCellular4Bar: Char = '\ue1c8'

    const val Outbound: Char = '\ue1ca'

    const val SocialDistance: Char = '\ue1cb'

    const val SafetyDivider: Char = '\ue1cc'

    const val SignalCellularConnectedNoInternet4Bar: Char = '\ue1cd'

    const val SignalCellularNull: Char = '\ue1cf'

    const val SignalCellularOff: Char = '\ue1d0'

    const val ProductionQuantityLimits: Char = '\ue1d1'

    const val Troubleshoot: Char = '\ue1d2'

    const val AddReaction: Char = '\ue1d3'

    const val HealthAndSafety: Char = '\ue1d5'

    const val SignalWifi4Bar: Char = '\ue1d8'

    const val WifiLock: Char = '\ue1d9'

    const val SignalWifiOff: Char = '\ue1da'

    const val Storage: Char = '\ue1db'

    const val TvGuide: Char = '\ue1dc'

    const val TvOptionsEditChannels: Char = '\ue1dd'

    const val TvOptionsInputSettings: Char = '\ue1de'

    const val Usb: Char = '\ue1e0'

    const val WifiTethering: Char = '\ue1e2'

    const val ActivityZone: Char = '\ue1e6'

    const val Doorbell3p: Char = '\ue1e7'

    const val DetectorStatus: Char = '\ue1e8'

    const val ToolsPowerDrill: Char = '\ue1e9'

    const val RangeHood: Char = '\ue1ea'

    const val Emergency: Char = '\ue1eb'

    const val TableLamp: Char = '\ue1f2'

    const val DoorbellChime: Char = '\ue1f3'

    const val Switch: Char = '\ue1f4'

    const val DetectorAlarm: Char = '\ue1f7'

    const val QuietTime: Char = '\ue1f9'

    const val AddToHomeScreen: Char = '\ue1fe'

    const val DeviceThermostat: Char = '\ue1ff'

    const val MobileFriendly: Char = '\ue200'

    const val MobileOff: Char = '\ue201'

    const val SignalCellularAlt: Char = '\ue202'

    const val Pergola: Char = '\ue203'

    const val DetectorBattery: Char = '\ue204'

    const val OutdoorGarden: Char = '\ue205'

    const val BatteryProfile: Char = '\ue206'

    const val EvStation: Char = '\ue209'

    const val Routine: Char = '\ue20c'

    const val Dresser: Char = '\ue210'

    const val Sleep: Char = '\ue213'

    const val AutoSchedule: Char = '\ue214'

    const val FamiliarFaceAndZone: Char = '\ue21c'

    const val Transportation: Char = '\ue21d'

    const val FloorLamp: Char = '\ue21e'

    const val DetectorOffline: Char = '\ue223'

    const val Valve: Char = '\ue224'

    const val AttachFile: Char = '\ue226'

    const val AttachMoney: Char = '\ue227'

    const val BorderAll: Char = '\ue228'

    const val BorderBottom: Char = '\ue229'

    const val BorderClear: Char = '\ue22a'

    const val BorderColor: Char = '\ue22b'

    const val BorderHorizontal: Char = '\ue22c'

    const val BorderInner: Char = '\ue22d'

    const val BorderLeft: Char = '\ue22e'

    const val BorderOuter: Char = '\ue22f'

    const val BorderRight: Char = '\ue230'

    const val BorderStyle: Char = '\ue231'

    const val BorderTop: Char = '\ue232'

    const val BorderVertical: Char = '\ue233'

    const val FormatAlignCenter: Char = '\ue234'

    const val FormatAlignJustify: Char = '\ue235'

    const val FormatAlignLeft: Char = '\ue236'

    const val FormatAlignRight: Char = '\ue237'

    const val FormatBold: Char = '\ue238'

    const val FormatClear: Char = '\ue239'

    const val FormatColorFill: Char = '\ue23a'

    const val FormatColorReset: Char = '\ue23b'

    const val FormatColorText: Char = '\ue23c'

    const val FormatIndentDecrease: Char = '\ue23d'

    const val FormatIndentIncrease: Char = '\ue23e'

    const val FormatItalic: Char = '\ue23f'

    const val FormatLineSpacing: Char = '\ue240'

    const val FormatListBulleted: Char = '\ue241'

    const val FormatListNumbered: Char = '\ue242'

    const val FormatPaint: Char = '\ue243'

    const val FormatQuote: Char = '\ue244'

    const val FormatSize: Char = '\ue245'

    const val FormatStrikethrough: Char = '\ue246'

    const val FormatTextdirectionLToR: Char = '\ue247'

    const val FormatTextdirectionRToL: Char = '\ue248'

    const val FormatUnderlined: Char = '\ue249'

    const val Functions: Char = '\ue24a'

    const val InsertChart: Char = '\ue24b'

    const val Mood: Char = '\ue24e'

    const val Event: Char = '\ue24f'

    const val Image: Char = '\ue251'

    const val MergeType: Char = '\ue252'

    const val ModeComment: Char = '\ue253'

    const val Publish: Char = '\ue255'

    const val SpaceBar: Char = '\ue256'

    const val StrikethroughS: Char = '\ue257'

    const val VerticalAlignBottom: Char = '\ue258'

    const val VerticalAlignCenter: Char = '\ue259'

    const val VerticalAlignTop: Char = '\ue25a'

    const val WrapText: Char = '\ue25b'

    const val MoneyOff: Char = '\ue25c'

    const val DragHandle: Char = '\ue25d'

    const val FormatShapes: Char = '\ue25e'

    const val Highlight: Char = '\ue25f'

    const val LinearScale: Char = '\ue260'

    const val ShortText: Char = '\ue261'

    const val TextFields: Char = '\ue262'

    const val MonetizationOn: Char = '\ue263'

    const val Title: Char = '\ue264'

    const val TableChart: Char = '\ue265'

    const val AddComment: Char = '\ue266'

    const val FormatListNumberedRtl: Char = '\ue267'

    const val ScatterPlot: Char = '\ue268'

    const val Score: Char = '\ue269'

    const val BarChart: Char = '\ue26b'

    const val Notes: Char = '\ue26c'

    const val Styler: Char = '\ue273'

    const val CoolToDry: Char = '\ue276'

    const val Gate: Char = '\ue277'

    const val Faucet: Char = '\ue278'

    const val Communication: Char = '\ue27c'

    const val HeatPumpBalance: Char = '\ue27e'

    const val Detector: Char = '\ue282'

    const val HomeIotDevice: Char = '\ue283'

    const val WaterHeater: Char = '\ue284'

    const val DetectorSmoke: Char = '\ue285'

    const val Blinds: Char = '\ue286'

    const val DoorSensor: Char = '\ue28a'

    const val LightGroup: Char = '\ue28b'

    const val NestTag: Char = '\ue28c'

    const val Mop: Char = '\ue28d'

    const val History: Char = '\ue28e'

    const val QuietTimeActive: Char = '\ue291'

    const val Multicooker: Char = '\ue293'

    const val HomeAppLogo: Char = '\ue295'

    const val Productivity: Char = '\ue296'

    const val Sprinkler: Char = '\ue29a'

    const val Airwave: Char = '\ue29c'

    const val DetectionAndZone: Char = '\ue29f'

    const val Scene: Char = '\ue2a7'

    const val Laundry: Char = '\ue2a8'

    const val ToolsPliersWireStripper: Char = '\ue2aa'

    const val ToolsInstallationKit: Char = '\ue2ab'

    const val SettopComponent: Char = '\ue2ac'

    const val Charger: Char = '\ue2ae'

    const val DetectorCo: Char = '\ue2af'

    const val WallLamp: Char = '\ue2b4'

    const val Cooking: Char = '\ue2b6'

    const val Kettle: Char = '\ue2b9'

    const val EarlyOn: Char = '\ue2ba'

    const val WindowSensor: Char = '\ue2bb'

    const val Attachment: Char = '\ue2bc'

    const val Cloud: Char = '\ue2bd'

    const val CloudCircle: Char = '\ue2be'

    const val CloudDone: Char = '\ue2bf'

    const val CloudDownload: Char = '\ue2c0'

    const val CloudOff: Char = '\ue2c1'

    const val CloudUpload: Char = '\ue2c3'

    const val FileMap: Char = '\ue2c5'

    const val Upload: Char = '\ue2c6'

    const val Folder: Char = '\ue2c7'

    const val FolderOpen: Char = '\ue2c8'

    const val FolderShared: Char = '\ue2c9'

    const val AirFreshener: Char = '\ue2ca'

    const val ToolsLadder: Char = '\ue2cb'

    const val CreateNewFolder: Char = '\ue2cc'

    const val WeatherSnowy: Char = '\ue2cd'

    const val TravelExplore: Char = '\ue2db'

    const val DataLossPrevention: Char = '\ue2dc'

    const val IdentityAwareProxy: Char = '\ue2dd'

    const val TaskAlt: Char = '\ue2e6'

    const val ChangeCircle: Char = '\ue2e7'

    const val ArrowBackIosNew: Char = '\ue2ea'

    const val Savings: Char = '\ue2eb'

    const val CopyAll: Char = '\ue2ec'

    const val Cast: Char = '\ue307'

    const val CastConnected: Char = '\ue308'

    const val Computer: Char = '\ue30a'

    const val DesktopMac: Char = '\ue30b'

    const val DesktopWindows: Char = '\ue30c'

    const val DeveloperBoard: Char = '\ue30d'

    const val Dock: Char = '\ue30e'

    const val Headphones: Char = '\ue310'

    const val HeadsetMic: Char = '\ue311'

    const val Keyboard: Char = '\ue312'

    const val KeyboardArrowDown: Char = '\ue313'

    const val KeyboardArrowLeft: Char = '\ue314'

    const val KeyboardArrowRight: Char = '\ue315'

    const val KeyboardArrowUp: Char = '\ue316'

    const val KeyboardBackspace: Char = '\ue317'

    const val KeyboardCapslock: Char = '\ue318'

    const val KeyboardHide: Char = '\ue31a'

    const val KeyboardReturn: Char = '\ue31b'

    const val KeyboardTab: Char = '\ue31c'

    const val LaptopChromebook: Char = '\ue31f'

    const val LaptopMac: Char = '\ue320'

    const val LaptopWindows: Char = '\ue321'

    const val Memory: Char = '\ue322'

    const val Mouse: Char = '\ue323'

    const val PhoneAndroid: Char = '\ue324'

    const val PhoneIphone: Char = '\ue325'

    const val PhonelinkOff: Char = '\ue327'

    const val Router: Char = '\ue328'

    const val Scanner: Char = '\ue329'

    const val Security: Char = '\ue32a'

    const val SimCard: Char = '\ue32b'

    const val Smartphone: Char = '\ue32c'

    const val Speaker: Char = '\ue32d'

    const val SpeakerGroup: Char = '\ue32e'

    const val Tablet: Char = '\ue32f'

    const val TabletAndroid: Char = '\ue330'

    const val TabletMac: Char = '\ue331'

    const val Toys: Char = '\ue332'

    const val Tv: Char = '\ue333'

    const val Watch: Char = '\ue334'

    const val DeviceHub: Char = '\ue335'

    const val PowerInput: Char = '\ue336'

    const val DevicesOther: Char = '\ue337'

    const val VideogameAsset: Char = '\ue338'

    const val DeviceUnknown: Char = '\ue339'

    const val HeadsetOff: Char = '\ue33a'

    const val AlignCenter: Char = '\ue356'

    const val DirectorySync: Char = '\ue394'

    const val NotificationAdd: Char = '\ue399'

    const val AddToPhotos: Char = '\ue39d'

    const val Adjust: Char = '\ue39e'

    const val Assistant: Char = '\ue39f'

    const val MusicNote: Char = '\ue3a1'

    const val BlurCircular: Char = '\ue3a2'

    const val BlurLinear: Char = '\ue3a3'

    const val BlurOff: Char = '\ue3a4'

    const val BlurOn: Char = '\ue3a5'

    const val Brightness1: Char = '\ue3a6'

    const val Brightness2: Char = '\ue3a7'

    const val Brightness3: Char = '\ue3a8'

    const val Brightness4: Char = '\ue3a9'

    const val Brightness5: Char = '\ue3aa'

    const val Brightness6: Char = '\ue3ab'

    const val Brightness7: Char = '\ue3ac'

    const val BrokenImage: Char = '\ue3ad'

    const val Brush: Char = '\ue3ae'

    const val Camera: Char = '\ue3af'

    const val PhotoCamera: Char = '\ue3b0'

    const val CameraFront: Char = '\ue3b1'

    const val CameraRear: Char = '\ue3b2'

    const val CameraRoll: Char = '\ue3b3'

    const val CenterFocusStrong: Char = '\ue3b4'

    const val CenterFocusWeak: Char = '\ue3b5'

    const val Filter: Char = '\ue3b6'

    const val Palette: Char = '\ue3b7'

    const val Colorize: Char = '\ue3b8'

    const val Compare: Char = '\ue3b9'

    const val ControlPointDuplicate: Char = '\ue3bb'

    const val Crop169: Char = '\ue3bc'

    const val Crop32: Char = '\ue3bd'

    const val Crop: Char = '\ue3be'

    const val Crop54: Char = '\ue3bf'

    const val Crop75: Char = '\ue3c0'

    const val CropSquare: Char = '\ue3c1'

    const val CropFree: Char = '\ue3c2'

    const val CropLandscape: Char = '\ue3c3'

    const val CropPortrait: Char = '\ue3c5'

    const val Dehaze: Char = '\ue3c7'

    const val Details: Char = '\ue3c8'

    const val Exposure: Char = '\ue3ca'

    const val ExposureNeg1: Char = '\ue3cb'

    const val ExposureNeg2: Char = '\ue3cc'

    const val ExposurePlus1: Char = '\ue3cd'

    const val ExposurePlus2: Char = '\ue3ce'

    const val ExposureZero: Char = '\ue3cf'

    const val Filter1: Char = '\ue3d0'

    const val Filter2: Char = '\ue3d1'

    const val Filter3: Char = '\ue3d2'

    const val Filter4: Char = '\ue3d4'

    const val Filter5: Char = '\ue3d5'

    const val Filter6: Char = '\ue3d6'

    const val Filter7: Char = '\ue3d7'

    const val Filter8: Char = '\ue3d8'

    const val Filter9: Char = '\ue3d9'

    const val Filter9Plus: Char = '\ue3da'

    const val FilterBAndW: Char = '\ue3db'

    const val FilterCenterFocus: Char = '\ue3dc'

    const val FilterDrama: Char = '\ue3dd'

    const val FilterFrames: Char = '\ue3de'

    const val FilterHdr: Char = '\ue3df'

    const val FilterNone: Char = '\ue3e0'

    const val FilterRetrolux: Char = '\ue3e1'

    const val FilterTiltShift: Char = '\ue3e2'

    const val FilterVintage: Char = '\ue3e3'

    const val Flare: Char = '\ue3e4'

    const val FlashAuto: Char = '\ue3e5'

    const val FlashOff: Char = '\ue3e6'

    const val FlashOn: Char = '\ue3e7'

    const val Flip: Char = '\ue3e8'

    const val Gradient: Char = '\ue3e9'

    const val Grain: Char = '\ue3ea'

    const val GridOff: Char = '\ue3eb'

    const val GridOn: Char = '\ue3ec'

    const val HdrOff: Char = '\ue3ed'

    const val HdrOn: Char = '\ue3ee'

    const val HdrPlusOff: Char = '\ue3ef'

    const val HdrStrong: Char = '\ue3f1'

    const val HdrWeak: Char = '\ue3f2'

    const val Healing: Char = '\ue3f3'

    const val ImageAspectRatio: Char = '\ue3f5'

    const val Landscape: Char = '\ue3f7'

    const val LeakAdd: Char = '\ue3f8'

    const val LeakRemove: Char = '\ue3f9'

    const val Looks3: Char = '\ue3fb'

    const val Looks: Char = '\ue3fc'

    const val Looks4: Char = '\ue3fd'

    const val Looks5: Char = '\ue3fe'

    const val Looks6: Char = '\ue3ff'

    const val LooksOne: Char = '\ue400'

    const val LooksTwo: Char = '\ue401'

    const val Loupe: Char = '\ue402'

    const val MonochromePhotos: Char = '\ue403'

    const val Nature: Char = '\ue406'

    const val NaturePeople: Char = '\ue407'

    const val ChevronLeft: Char = '\ue408'

    const val ChevronRight: Char = '\ue409'

    const val Panorama: Char = '\ue40b'

    const val PanoramaFishEye: Char = '\ue40c'

    const val PanoramaHorizontal: Char = '\ue40d'

    const val PanoramaVertical: Char = '\ue40e'

    const val PanoramaWideAngle: Char = '\ue40f'

    const val Photo: Char = '\ue410'

    const val PhotoAlbum: Char = '\ue411'

    const val PhotoLibrary: Char = '\ue413'

    const val PictureAsPdf: Char = '\ue415'

    const val AccountBox: Char = '\ue416'

    const val Visibility: Char = '\ue417'

    const val Rotate90DegreesCcw: Char = '\ue418'

    const val RotateLeft: Char = '\ue419'

    const val RotateRight: Char = '\ue41a'

    const val Slideshow: Char = '\ue41b'

    const val Straighten: Char = '\ue41c'

    const val Style: Char = '\ue41d'

    const val SwitchCamera: Char = '\ue41e'

    const val SwitchVideo: Char = '\ue41f'

    const val Texture: Char = '\ue421'

    const val Timelapse: Char = '\ue422'

    const val Timer10: Char = '\ue423'

    const val Timer3: Char = '\ue424'

    const val Timer: Char = '\ue425'

    const val TimerOff: Char = '\ue426'

    const val Tonality: Char = '\ue427'

    const val Transform: Char = '\ue428'

    const val Tune: Char = '\ue429'

    const val ViewComfy: Char = '\ue42a'

    const val ViewCompact: Char = '\ue42b'

    const val WbAuto: Char = '\ue42c'

    const val WbIncandescent: Char = '\ue42e'

    const val WbSunny: Char = '\ue430'

    const val CollectionsBookmark: Char = '\ue431'

    const val PhotoSizeSelectLarge: Char = '\ue433'

    const val PhotoSizeSelectSmall: Char = '\ue434'

    const val Vignette: Char = '\ue435'

    const val WbIridescent: Char = '\ue436'

    const val CropRotate: Char = '\ue437'

    const val LinkedCamera: Char = '\ue438'

    const val AddAPhoto: Char = '\ue439'

    const val MovieFilter: Char = '\ue43a'

    const val PhotoFilter: Char = '\ue43b'

    const val BurstMode: Char = '\ue43c'

    const val ShutterSpeed: Char = '\ue43d'

    const val AddPhotoAlternate: Char = '\ue43e'

    const val ImageSearch: Char = '\ue43f'

    const val MusicOff: Char = '\ue440'

    const val QuickReference: Char = '\ue46e'

    const val ChartData: Char = '\ue473'

    const val OtherAdmission: Char = '\ue47b'

    const val Fluid: Char = '\ue483'

    const val Demography: Char = '\ue489'

    const val AdminMeds: Char = '\ue48d'

    const val Package: Char = '\ue48f'

    const val ErrorMed: Char = '\ue49b'

    const val Glucose: Char = '\ue4a0'

    const val Overview: Char = '\ue4a7'

    const val HomeHealth: Char = '\ue4b9'

    const val MixtureMed: Char = '\ue4c8'

    const val Wifi1Bar: Char = '\ue4ca'

    const val Acute: Char = '\ue4cb'

    const val ShortStay: Char = '\ue4d0'

    const val Wifi2Bar: Char = '\ue4d9'

    const val OxygenSaturation: Char = '\ue4de'

    const val Man: Char = '\ue4eb'

    const val CodeOff: Char = '\ue4f3'

    const val CreditCardOff: Char = '\ue4f4'

    const val ExtensionOff: Char = '\ue4f5'

    const val OpenInNewOff: Char = '\ue4f6'

    const val WebAssetOff: Char = '\ue4f7'

    const val ContentPasteOff: Char = '\ue4f8'

    const val FontDownloadOff: Char = '\ue4f9'

    const val UsbOff: Char = '\ue4fa'

    const val AutoGraph: Char = '\ue4fb'

    const val QueryStats: Char = '\ue4fc'

    const val Schema: Char = '\ue4fd'

    const val FileDownloadOff: Char = '\ue4fe'

    const val DeveloperBoardOff: Char = '\ue4ff'

    const val VideogameAssetOff: Char = '\ue500'

    const val Moving: Char = '\ue501'

    const val Sailing: Char = '\ue502'

    const val Snowmobile: Char = '\ue503'

    const val FileSaveOff: Char = '\ue505'

    const val SelectWindowOff: Char = '\ue506'

    const val DownhillSkiing: Char = '\ue509'

    const val Hiking: Char = '\ue50a'

    const val IceSkating: Char = '\ue50b'

    const val Kayaking: Char = '\ue50c'

    const val Kitesurfing: Char = '\ue50d'

    const val NordicWalking: Char = '\ue50e'

    const val Paragliding: Char = '\ue50f'

    const val PersonOff: Char = '\ue510'

    const val Skateboarding: Char = '\ue511'

    const val Sledding: Char = '\ue512'

    const val Snowboarding: Char = '\ue513'

    const val Snowshoeing: Char = '\ue514'

    const val Surfing: Char = '\ue515'

    const val LightMode: Char = '\ue518'

    const val PerformanceMax: Char = '\ue51a'

    const val DarkMode: Char = '\ue51c'

    const val RunningWithErrors: Char = '\ue51d'

    const val Sensors: Char = '\ue51e'

    const val SensorsOff: Char = '\ue51f'

    const val PianoOff: Char = '\ue520'

    const val Piano: Char = '\ue521'

    const val EditNotifications: Char = '\ue525'

    const val SourceEnvironment: Char = '\ue527'

    const val Beenhere: Char = '\ue52d'

    const val Directions: Char = '\ue52e'

    const val DirectionsBike: Char = '\ue52f'

    const val DirectionsBus: Char = '\ue530'

    const val DirectionsCar: Char = '\ue531'

    const val DirectionsBoat: Char = '\ue532'

    const val DirectionsSubway: Char = '\ue533'

    const val DirectionsRailway: Char = '\ue534'

    const val DirectionsWalk: Char = '\ue536'

    const val ExploreNearby: Char = '\ue538'

    const val Flight: Char = '\ue539'

    const val Hotel: Char = '\ue53a'

    const val Layers: Char = '\ue53b'

    const val LayersClear: Char = '\ue53c'

    const val LocalAtm: Char = '\ue53e'

    const val LocalActivity: Char = '\ue53f'

    const val LocalBar: Char = '\ue540'

    const val LocalCafe: Char = '\ue541'

    const val LocalCarWash: Char = '\ue542'

    const val LocalConvenienceStore: Char = '\ue543'

    const val LocalDrink: Char = '\ue544'

    const val LocalFlorist: Char = '\ue545'

    const val LocalGasStation: Char = '\ue546'

    const val ShoppingCart: Char = '\ue547'

    const val LocalHospital: Char = '\ue548'

    const val LocalLaundryService: Char = '\ue54a'

    const val LocalLibrary: Char = '\ue54b'

    const val LocalMall: Char = '\ue54c'

    const val Theaters: Char = '\ue54d'

    const val Sell: Char = '\ue54e'

    const val LocalParking: Char = '\ue54f'

    const val LocalPharmacy: Char = '\ue550'

    const val LocalPizza: Char = '\ue552'

    const val LocalPostOffice: Char = '\ue554'

    const val Print: Char = '\ue555'

    const val RestaurantMenu: Char = '\ue556'

    const val LocalSee: Char = '\ue557'

    const val LocalShipping: Char = '\ue558'

    const val LocalTaxi: Char = '\ue559'

    const val PersonPin: Char = '\ue55a'

    const val Map: Char = '\ue55b'

    const val Navigation: Char = '\ue55d'

    const val PinDrop: Char = '\ue55e'

    const val RateReview: Char = '\ue560'

    const val Satellite: Char = '\ue562'

    const val Store: Char = '\ue563'

    const val Traffic: Char = '\ue565'

    const val DirectionsRun: Char = '\ue566'

    const val AddLocation: Char = '\ue567'

    const val EditLocation: Char = '\ue568'

    const val NearMe: Char = '\ue569'

    const val PersonPinCircle: Char = '\ue56a'

    const val ZoomOutMap: Char = '\ue56b'

    const val Restaurant: Char = '\ue56c'

    const val Streetview: Char = '\ue56e'

    const val Subway: Char = '\ue56f'

    const val Train: Char = '\ue570'

    const val Tram: Char = '\ue571'

    const val TransferWithinAStation: Char = '\ue572'

    const val Atm: Char = '\ue573'

    const val Category: Char = '\ue574'

    const val NotListedLocation: Char = '\ue575'

    const val DepartureBoard: Char = '\ue576'

    const val _360: Char = '\ue577'

    const val EditAttributes: Char = '\ue578'

    const val TransitEnterexit: Char = '\ue579'

    const val Fastfood: Char = '\ue57a'

    const val TripOrigin: Char = '\ue57b'

    const val CompassCalibration: Char = '\ue57c'

    const val Money: Char = '\ue57d'

    const val Iron: Char = '\ue583'

    const val Houseboat: Char = '\ue584'

    const val Chalet: Char = '\ue585'

    const val Villa: Char = '\ue586'

    const val Cottage: Char = '\ue587'

    const val Crib: Char = '\ue588'

    const val Cabin: Char = '\ue589'

    const val HolidayVillage: Char = '\ue58a'

    const val Gite: Char = '\ue58b'

    const val OtherHouses: Char = '\ue58c'

    const val Transgender: Char = '\ue58d'

    const val Male: Char = '\ue58e'

    const val Balcony: Char = '\ue58f'

    const val Female: Char = '\ue590'

    const val Bungalow: Char = '\ue591'

    const val Encrypted: Char = '\ue593'

    const val MovedLocation: Char = '\ue594'

    const val WebStories: Char = '\ue595'

    const val BookmarkAdd: Char = '\ue598'

    const val BookmarkAdded: Char = '\ue599'

    const val BookmarkRemove: Char = '\ue59a'

    const val Apps: Char = '\ue5c3'

    const val ArrowBack: Char = '\ue5c4'

    const val ArrowDropDown: Char = '\ue5c5'

    const val ArrowDropDownCircle: Char = '\ue5c6'

    const val ArrowDropUp: Char = '\ue5c7'

    const val ArrowForward: Char = '\ue5c8'

    const val Cancel: Char = '\ue5c9'

    const val Check: Char = '\ue5ca'

    const val ExpandLess: Char = '\ue5ce'

    const val ExpandMore: Char = '\ue5cf'

    const val Fullscreen: Char = '\ue5d0'

    const val FullscreenExit: Char = '\ue5d1'

    const val Menu: Char = '\ue5d2'

    const val MoreHoriz: Char = '\ue5d3'

    const val MoreVert: Char = '\ue5d4'

    const val Refresh: Char = '\ue5d5'

    const val UnfoldLess: Char = '\ue5d6'

    const val UnfoldMore: Char = '\ue5d7'

    const val ArrowUpward: Char = '\ue5d8'

    const val SubdirectoryArrowLeft: Char = '\ue5d9'

    const val SubdirectoryArrowRight: Char = '\ue5da'

    const val ArrowDownward: Char = '\ue5db'

    const val FirstPage: Char = '\ue5dc'

    const val LastPage: Char = '\ue5dd'

    const val ArrowLeft: Char = '\ue5de'

    const val ArrowRight: Char = '\ue5df'

    const val ArrowBackIos: Char = '\ue5e0'

    const val ArrowForwardIos: Char = '\ue5e1'

    const val Shift: Char = '\ue5f2'

    const val Emoticon: Char = '\ue5f3'

    const val ShareEta: Char = '\ue5f7'

    const val DocumentScanner: Char = '\ue5fa'

    const val ExpansionPanels: Char = '\ue600'

    const val Shapes: Char = '\ue602'

    const val NewLabel: Char = '\ue609'

    const val Adb: Char = '\ue60e'

    const val DiscFull: Char = '\ue610'

    const val DoNotDisturb: Char = '\ue612'

    const val EventAvailable: Char = '\ue614'

    const val EventBusy: Char = '\ue615'

    const val EventNote: Char = '\ue616'

    const val FolderSpecial: Char = '\ue617'

    const val Mms: Char = '\ue618'

    const val More: Char = '\ue619'

    const val NetworkLocked: Char = '\ue61a'

    const val PhoneBluetoothSpeaker: Char = '\ue61b'

    const val PhoneForwarded: Char = '\ue61c'

    const val PhoneInTalk: Char = '\ue61d'

    const val PhoneLocked: Char = '\ue61e'

    const val PhoneMissed: Char = '\ue61f'

    const val PhonePaused: Char = '\ue620'

    const val SdCardAlert: Char = '\ue624'

    const val SmsFailed: Char = '\ue626'

    const val Sync: Char = '\ue627'

    const val SyncDisabled: Char = '\ue628'

    const val SyncProblem: Char = '\ue629'

    const val SystemUpdate: Char = '\ue62a'

    const val TapAndPlay: Char = '\ue62b'

    const val Vibration: Char = '\ue62d'

    const val VoiceChat: Char = '\ue62e'

    const val VpnLock: Char = '\ue62f'

    const val AirlineSeatFlat: Char = '\ue630'

    const val AirlineSeatFlatAngled: Char = '\ue631'

    const val AirlineSeatIndividualSuite: Char = '\ue632'

    const val AirlineSeatLegroomExtra: Char = '\ue633'

    const val AirlineSeatLegroomNormal: Char = '\ue634'

    const val AirlineSeatLegroomReduced: Char = '\ue635'

    const val AirlineSeatReclineExtra: Char = '\ue636'

    const val AirlineSeatReclineNormal: Char = '\ue637'

    const val ConfirmationNumber: Char = '\ue638'

    const val LiveTv: Char = '\ue639'

    const val Power: Char = '\ue63c'

    const val Wc: Char = '\ue63d'

    const val Wifi: Char = '\ue63e'

    const val EnhancedEncryption: Char = '\ue63f'

    const val NetworkCheck: Char = '\ue640'

    const val NoEncryption: Char = '\ue641'

    const val RvHookup: Char = '\ue642'

    const val DoNotDisturbOff: Char = '\ue643'

    const val PriorityHigh: Char = '\ue645'

    const val PowerOff: Char = '\ue646'

    const val TvOff: Char = '\ue647'

    const val WifiOff: Char = '\ue648'

    const val PhoneCallback: Char = '\ue649'

    const val Globe: Char = '\ue64c'

    const val Allergy: Char = '\ue64e'

    const val VitalSigns: Char = '\ue650'

    const val Procedure: Char = '\ue651'

    const val PatientList: Char = '\ue653'

    const val Pacemaker: Char = '\ue656'

    const val AccountChildInvert: Char = '\ue659'

    const val Ad: Char = '\ue65a'

    const val AdGroup: Char = '\ue65b'

    const val AddToDrive: Char = '\ue65c'

    const val AutoAwesome: Char = '\ue65f'

    const val AutoAwesomeMosaic: Char = '\ue660'

    const val AutoAwesomeMotion: Char = '\ue661'

    const val AutoFix: Char = '\ue662'

    const val AutoFixNormal: Char = '\ue664'

    const val AutoFixOff: Char = '\ue665'

    const val AutoStories: Char = '\ue666'

    const val BidLandscape: Char = '\ue667'

    const val BigtopUpdates: Char = '\ue669'

    const val SpaceDashboard: Char = '\ue66b'

    const val DriveFileMove: Char = '\ue675'

    const val Experiment: Char = '\ue686'

    const val NestProtect: Char = '\ue68e'

    const val NestThermostat: Char = '\ue68f'

    const val PlannerBannerAdPt: Char = '\ue692'

    const val PlannerReview: Char = '\ue694'

    const val Reminder: Char = '\ue695'

    const val SearchHandsFree: Char = '\ue696'

    const val Stat0: Char = '\ue697'

    const val Stat1: Char = '\ue698'

    const val Stat2: Char = '\ue699'

    const val Stat3: Char = '\ue69a'

    const val StatMinus1: Char = '\ue69b'

    const val StatMinus2: Char = '\ue69c'

    const val StatMinus3: Char = '\ue69d'

    const val SwapDrivingApps: Char = '\ue69e'

    const val SwapDrivingAppsWheel: Char = '\ue69f'

    const val ContrastCircle: Char = '\ue6a2'

    const val Unknown5: Char = '\ue6a5'

    const val TextUp: Char = '\ue6a7'

    const val Keep: Char = '\ue6aa'

    const val Sweep: Char = '\ue6ac'

    const val Checklist: Char = '\ue6b1'

    const val ChecklistRtl: Char = '\ue6b3'

    const val Nearby: Char = '\ue6b7'

    const val IosShare: Char = '\ue6b8'

    const val Finance: Char = '\ue6bf'

    const val NotificationMultiple: Char = '\ue6c2'

    const val OnHubDevice: Char = '\ue6c3'

    const val PieChart: Char = '\ue6c4'

    const val StackedEmail: Char = '\ue6c7'

    const val StackedInbox: Char = '\ue6c9'

    const val Travel: Char = '\ue6ca'

    const val Csv: Char = '\ue6cf'

    const val InkEraser: Char = '\ue6d0'

    const val InkHighlighter: Char = '\ue6d1'

    const val InkMarker: Char = '\ue6d2'

    const val InkPen: Char = '\ue6d3'

    const val InkSelection: Char = '\ue6d4'

    const val Tsv: Char = '\ue6d6'

    const val PersonalInjury: Char = '\ue6da'

    const val BubbleChart: Char = '\ue6dd'

    const val GeneralDevice: Char = '\ue6de'

    const val MultilineChart: Char = '\ue6df'

    const val ShowChart: Char = '\ue6e1'

    const val Ods: Char = '\ue6e8'

    const val Odt: Char = '\ue6e9'

    const val Hallway: Char = '\ue6f8'

    const val SelectWindow: Char = '\ue6fa'

    const val Trip: Char = '\ue6fb'

    const val MissingController: Char = '\ue701'

    const val OpenInPhone: Char = '\ue702'

    const val PersonalPlaces: Char = '\ue703'

    const val Post: Char = '\ue705'

    const val RateReviewRtl: Char = '\ue706'

    const val UserAttributes: Char = '\ue708'

    const val Barcode: Char = '\ue70b'

    const val BarcodeScanner: Char = '\ue70c'

    const val Checkbook: Char = '\ue70d'

    const val Enterprise: Char = '\ue70e'

    const val NoSound: Char = '\ue710'

    const val GarageDoor: Char = '\ue714'

    const val GoogleHomeDevices: Char = '\ue715'

    const val ServiceToolbox: Char = '\ue717'

    const val Target: Char = '\ue719'

    const val Trophy: Char = '\ue71a'

    const val TvSignin: Char = '\ue71b'

    const val Animation: Char = '\ue71c'

    const val AutoTowing: Char = '\ue71e'

    const val Sdk: Char = '\ue720'

    const val Manufacturing: Char = '\ue726'

    const val TextAd: Char = '\ue728'

    const val AddBusiness: Char = '\ue729'

    const val AddAd: Char = '\ue72a'

    const val BottomDrawer: Char = '\ue72d'

    const val MaskedTransitions: Char = '\ue72e'

    const val ButtonsAlt: Char = '\ue72f'

    const val BottomAppBar: Char = '\ue730'

    const val PageControl: Char = '\ue731'

    const val CustomTypography: Char = '\ue732'

    const val Switches: Char = '\ue733'

    const val UniversalCurrencyAlt: Char = '\ue734'

    const val RealEstateAgent: Char = '\ue73a'

    const val Key: Char = '\ue73c'

    const val MovingBeds: Char = '\ue73d'

    const val MovingMinistry: Char = '\ue73e'

    const val Move: Char = '\ue740'

    const val MoveLocation: Char = '\ue741'

    const val EditCalendar: Char = '\ue742'

    const val HotelClass: Char = '\ue743'

    const val PrivateConnectivity: Char = '\ue744'

    const val EditNote: Char = '\ue745'

    const val Draw: Char = '\ue746'

    const val GroupOff: Char = '\ue747'

    const val FreeCancellation: Char = '\ue748'

    const val GeneratingTokens: Char = '\ue749'

    const val Recycling: Char = '\ue760'

    const val Compost: Char = '\ue761'

    const val AdsClick: Char = '\ue762'

    const val PinInvoke: Char = '\ue763'

    const val BackHand: Char = '\ue764'

    const val WavingHand: Char = '\ue766'

    const val PinEnd: Char = '\ue767'

    const val FrontHand: Char = '\ue769'

    const val DisabledVisible: Char = '\ue76e'

    const val DataExploration: Char = '\ue76f'

    const val AreaChart: Char = '\ue770'

    const val ZonePersonIdle: Char = '\ue77a'

    const val ToolsLevel: Char = '\ue77b'

    const val DoorOpen: Char = '\ue77c'

    const val WindowClosed: Char = '\ue77e'

    const val ZonePersonAlert: Char = '\ue781'

    const val MotionSensorIdle: Char = '\ue783'

    const val MotionSensorAlert: Char = '\ue784'

    const val TvWithAssistant: Char = '\ue785'

    const val HouseWithShield: Char = '\ue786'

    const val ZonePersonUrgent: Char = '\ue788'

    const val NestMini: Char = '\ue789'

    const val ArmingCountdown: Char = '\ue78a'

    const val WindowOpen: Char = '\ue78c'

    const val ShieldWithHouse: Char = '\ue78d'

    const val MotionSensorUrgent: Char = '\ue78e'

    const val ShieldWithHeart: Char = '\ue78f'

    const val MotionSensorActive: Char = '\ue792'

    const val WaterDrop: Char = '\ue798'

    const val CrueltyFree: Char = '\ue799'

    const val TipsAndUpdates: Char = '\ue79a'

    const val IncompleteCircle: Char = '\ue79b'

    const val VolumeDownAlt: Char = '\ue79c'

    const val CommentsDisabled: Char = '\ue7a2'

    const val GifBox: Char = '\ue7a3'

    const val GroupRemove: Char = '\ue7ad'

    const val WorkspacePremium: Char = '\ue7af'

    const val Co2: Char = '\ue7b0'

    const val DraftOrders: Char = '\ue7b3'

    const val Interests: Char = '\ue7c8'

    const val ConnectingAirports: Char = '\ue7c9'

    const val Airlines: Char = '\ue7ca'

    const val FlightClass: Char = '\ue7cb'

    const val AppsOutage: Char = '\ue7cc'

    const val ExpandCircleDown: Char = '\ue7cd'

    const val ModeOfTravel: Char = '\ue7ce'

    const val BrowserUpdated: Char = '\ue7cf'

    const val AirlineStops: Char = '\ue7d0'

    const val QuickPhrases: Char = '\ue7d1'

    const val SoupKitchen: Char = '\ue7d3'

    const val Preliminary: Char = '\ue7d8'

    const val SwitchAccessShortcut: Char = '\ue7e1'

    const val SwitchAccessShortcutAdd: Char = '\ue7e2'

    const val InkEraserOff: Char = '\ue7e3'

    const val SouthAmerica: Char = '\ue7e4'

    const val PlaylistAddCircle: Char = '\ue7e5'

    const val PlaylistAddCheckCircle: Char = '\ue7e6'

    const val Cake: Char = '\ue7e9'

    const val Circles: Char = '\ue7ea'

    const val CirclesExt: Char = '\ue7ec'

    const val Communities: Char = '\ue7ed'

    const val Group: Char = '\ue7ef'

    const val GroupAdd: Char = '\ue7f0'

    const val LocationCity: Char = '\ue7f1'

    const val MoodBad: Char = '\ue7f3'

    const val Notifications: Char = '\ue7f4'

    const val NotificationsOff: Char = '\ue7f6'

    const val NotificationsActive: Char = '\ue7f7'

    const val NotificationsPaused: Char = '\ue7f8'

    const val Pages: Char = '\ue7f9'

    const val PartyMode: Char = '\ue7fa'

    const val Person: Char = '\ue7fd'

    const val PersonAdd: Char = '\ue7fe'

    const val Public: Char = '\ue80b'

    const val School: Char = '\ue80c'

    const val Share: Char = '\ue80d'

    const val Whatshot: Char = '\ue80e'

    const val Snowing: Char = '\ue80f'

    const val CloudySnowing: Char = '\ue810'

    const val SentimentDissatisfied: Char = '\ue811'

    const val SentimentNeutral: Char = '\ue812'

    const val SentimentVeryDissatisfied: Char = '\ue814'

    const val SentimentVerySatisfied: Char = '\ue815'

    const val ThumbDown: Char = '\ue816'

    const val ThumbUp: Char = '\ue817'

    const val Foggy: Char = '\ue818'

    const val SunnySnowing: Char = '\ue819'

    const val Sunny: Char = '\ue81a'

    const val Blanket: Char = '\ue828'

    const val AirPurifierGen: Char = '\ue829'

    const val EmergencyHome: Char = '\ue82a'

    const val NestHelloDoorbell: Char = '\ue82c'

    const val GarageHome: Char = '\ue82d'

    const val TamperDetectionOff: Char = '\ue82e'

    const val TvGen: Char = '\ue830'

    const val DishwasherGen: Char = '\ue832'

    const val InHomeMode: Char = '\ue833'

    const val CheckBox: Char = '\ue834'

    const val CheckBoxOutlineBlank: Char = '\ue835'

    const val RadioButtonUnchecked: Char = '\ue836'

    const val RadioButtonChecked: Char = '\ue837'

    const val Star: Char = '\ue838'

    const val StarHalf: Char = '\ue839'

    const val InterpreterMode: Char = '\ue83b'

    const val ChromecastDevice: Char = '\ue83c'

    const val ControllerGen: Char = '\ue83d'

    const val RemoteGen: Char = '\ue83e'

    const val NestWifiPoint: Char = '\ue83f'

    const val GoogleWifi: Char = '\ue840'

    const val NestWifiRouter: Char = '\ue841'

    const val KebabDining: Char = '\ue842'

    const val OvenGen: Char = '\ue843'

    const val SmartOutlet: Char = '\ue844'

    const val Thermometer: Char = '\ue846'

    const val MicrowaveGen: Char = '\ue847'

    const val HomeMaxDots: Char = '\ue849'

    const val BlurMedium: Char = '\ue84c'

    const val _3dRotation: Char = '\ue84d'

    const val Accessibility: Char = '\ue84e'

    const val AccountBalance: Char = '\ue84f'

    const val AccountBalanceWallet: Char = '\ue850'

    const val AccountChild: Char = '\ue852'

    const val AccountCircle: Char = '\ue853'

    const val AddShoppingCart: Char = '\ue854'

    const val AlarmOff: Char = '\ue857'

    const val AlarmOn: Char = '\ue858'

    const val Android: Char = '\ue859'

    const val AspectRatio: Char = '\ue85b'

    const val Assignment: Char = '\ue85d'

    const val AssignmentInd: Char = '\ue85e'

    const val AssignmentLate: Char = '\ue85f'

    const val AssignmentReturn: Char = '\ue860'

    const val AssignmentReturned: Char = '\ue861'

    const val AssignmentTurnedIn: Char = '\ue862'

    const val Backup: Char = '\ue864'

    const val Book: Char = '\ue865'

    const val Bookmark: Char = '\ue866'

    const val BugReport: Char = '\ue868'

    const val Build: Char = '\ue869'

    const val Cached: Char = '\ue86a'

    const val ChangeHistory: Char = '\ue86b'

    const val CheckCircle: Char = '\ue86c'

    const val ChromeReaderMode: Char = '\ue86d'

    const val Code: Char = '\ue86f'

    const val CreditCard: Char = '\ue870'

    const val Dashboard: Char = '\ue871'

    const val Delete: Char = '\ue872'

    const val Description: Char = '\ue873'

    const val DeveloperModeTv: Char = '\ue874'

    const val Dns: Char = '\ue875'

    const val Done: Char = '\ue876'

    const val DoneAll: Char = '\ue877'

    const val ExitToApp: Char = '\ue879'

    const val Explore: Char = '\ue87a'

    const val Extension: Char = '\ue87b'

    const val Face: Char = '\ue87c'

    const val Favorite: Char = '\ue87d'

    const val FindInPage: Char = '\ue880'

    const val FindReplace: Char = '\ue881'

    const val FlipToBack: Char = '\ue882'

    const val FlipToFront: Char = '\ue883'

    const val Grade: Char = '\ue885'

    const val GroupWork: Char = '\ue886'

    const val Help: Char = '\ue887'

    const val Home: Char = '\ue88a'

    const val HourglassEmpty: Char = '\ue88b'

    const val HourglassFull: Char = '\ue88c'

    const val Lock: Char = '\ue88d'

    const val Info: Char = '\ue88e'

    const val Input: Char = '\ue890'

    const val InvertColors: Char = '\ue891'

    const val Label: Char = '\ue892'

    const val Language: Char = '\ue894'

    const val OpenInNew: Char = '\ue895'

    const val List: Char = '\ue896'

    const val LockOpen: Char = '\ue898'

    const val Loyalty: Char = '\ue89a'

    const val MarkunreadMailbox: Char = '\ue89b'

    const val NoteAdd: Char = '\ue89c'

    const val OpenInBrowser: Char = '\ue89d'

    const val OpenWith: Char = '\ue89f'

    const val Pageview: Char = '\ue8a0'

    const val PermCameraMic: Char = '\ue8a2'

    const val PermContactCalendar: Char = '\ue8a3'

    const val PermDataSetting: Char = '\ue8a4'

    const val PermDeviceInformation: Char = '\ue8a5'

    const val PermMedia: Char = '\ue8a7'

    const val PermPhoneMsg: Char = '\ue8a8'

    const val PermScanWifi: Char = '\ue8a9'

    const val PictureInPicture: Char = '\ue8aa'

    const val Polymer: Char = '\ue8ab'

    const val PowerSettingsNew: Char = '\ue8ac'

    const val Receipt: Char = '\ue8b0'

    const val Redeem: Char = '\ue8b1'

    const val Search: Char = '\ue8b6'

    const val SendMoney: Char = '\ue8b7'

    const val Settings: Char = '\ue8b8'

    const val SettingsApplications: Char = '\ue8b9'

    const val SettingsBackupRestore: Char = '\ue8ba'

    const val SettingsBluetooth: Char = '\ue8bb'

    const val SettingsCell: Char = '\ue8bc'

    const val SettingsBrightness: Char = '\ue8bd'

    const val SettingsEthernet: Char = '\ue8be'

    const val SettingsInputAntenna: Char = '\ue8bf'

    const val SettingsInputComponent: Char = '\ue8c0'

    const val SettingsInputHdmi: Char = '\ue8c2'

    const val SettingsInputSvideo: Char = '\ue8c3'

    const val SettingsOverscan: Char = '\ue8c4'

    const val SettingsPhone: Char = '\ue8c5'

    const val SettingsPower: Char = '\ue8c6'

    const val SettingsRemote: Char = '\ue8c7'

    const val SettingsVoice: Char = '\ue8c8'

    const val Shop: Char = '\ue8c9'

    const val ShoppingBasket: Char = '\ue8cb'

    const val SpeakerNotes: Char = '\ue8cd'

    const val Spellcheck: Char = '\ue8ce'

    const val BlurShort: Char = '\ue8cf'

    const val Stars: Char = '\ue8d0'

    const val Subject: Char = '\ue8d2'

    const val SupervisorAccount: Char = '\ue8d3'

    const val SwapHoriz: Char = '\ue8d4'

    const val SwapVerticalCircle: Char = '\ue8d6'

    const val SystemUpdateAlt: Char = '\ue8d7'

    const val Tab: Char = '\ue8d8'

    const val TabUnselected: Char = '\ue8d9'

    const val ThumbsUpDown: Char = '\ue8dd'

    const val Toc: Char = '\ue8de'

    const val Today: Char = '\ue8df'

    const val Toll: Char = '\ue8e0'

    const val TrackChanges: Char = '\ue8e1'

    const val Translate: Char = '\ue8e2'

    const val TrendingDown: Char = '\ue8e3'

    const val TrendingFlat: Char = '\ue8e4'

    const val TrendingUp: Char = '\ue8e5'

    const val VerifiedUser: Char = '\ue8e8'

    const val ViewAgenda: Char = '\ue8e9'

    const val ViewArray: Char = '\ue8ea'

    const val ViewCarousel: Char = '\ue8eb'

    const val ViewColumn: Char = '\ue8ec'

    const val ViewDay: Char = '\ue8ed'

    const val ViewHeadline: Char = '\ue8ee'

    const val ViewList: Char = '\ue8ef'

    const val ViewModule: Char = '\ue8f0'

    const val ViewQuilt: Char = '\ue8f1'

    const val ViewStream: Char = '\ue8f2'

    const val ViewWeek: Char = '\ue8f3'

    const val VisibilityOff: Char = '\ue8f5'

    const val CardMembership: Char = '\ue8f7'

    const val CardTravel: Char = '\ue8f8'

    const val Work: Char = '\ue8f9'

    const val YoutubeSearchedFor: Char = '\ue8fa'

    const val Eject: Char = '\ue8fb'

    const val CameraEnhance: Char = '\ue8fc'

    const val Reorder: Char = '\ue8fe'

    const val ZoomIn: Char = '\ue8ff'

    const val ZoomOut: Char = '\ue900'

    const val Http: Char = '\ue902'

    const val EventSeat: Char = '\ue903'

    const val FlightLand: Char = '\ue904'

    const val FlightTakeoff: Char = '\ue905'

    const val PlayForWork: Char = '\ue906'

    const val Matter: Char = '\ue907'

    const val Gif: Char = '\ue908'

    const val IndeterminateCheckBox: Char = '\ue909'

    const val OfflinePin: Char = '\ue90a'

    const val AllOut: Char = '\ue90b'

    const val Copyright: Char = '\ue90c'

    const val Fingerprint: Char = '\ue90d'

    const val Gavel: Char = '\ue90e'

    const val PictureInPictureAlt: Char = '\ue911'

    const val ImportantDevices: Char = '\ue912'

    const val TouchApp: Char = '\ue913'

    const val Accessible: Char = '\ue914'

    const val CompareArrows: Char = '\ue915'

    const val DateRange: Char = '\ue916'

    const val DonutLarge: Char = '\ue917'

    const val DonutSmall: Char = '\ue918'

    const val LineStyle: Char = '\ue919'

    const val LineWeight: Char = '\ue91a'

    const val Motorcycle: Char = '\ue91b'

    const val Opacity: Char = '\ue91c'

    const val Pets: Char = '\ue91d'

    const val Pregnancy: Char = '\ue91e'

    const val RecordVoiceOver: Char = '\ue91f'

    const val RoundedCorner: Char = '\ue920'

    const val Rowing: Char = '\ue921'

    const val Timeline: Char = '\ue922'

    const val Update: Char = '\ue923'

    const val PanTool: Char = '\ue925'

    const val EuroSymbol: Char = '\ue926'

    const val GTranslate: Char = '\ue927'

    const val RemoveShoppingCart: Char = '\ue928'

    const val RestorePage: Char = '\ue929'

    const val SpeakerNotesOff: Char = '\ue92a'

    const val DeleteForever: Char = '\ue92b'

    const val AccessibilityNew: Char = '\ue92c'

    const val DoneOutline: Char = '\ue92f'

    const val Maximize: Char = '\ue930'

    const val Minimize: Char = '\ue931'

    const val OfflineBolt: Char = '\ue932'

    const val SwapHorizontalCircle: Char = '\ue933'

    const val AccessibleForward: Char = '\ue934'

    const val CalendarToday: Char = '\ue935'

    const val CalendarViewDay: Char = '\ue936'

    const val LabelImportant: Char = '\ue937'

    const val RestoreFromTrash: Char = '\ue938'

    const val SupervisedUserCircle: Char = '\ue939'

    const val TextRotateUp: Char = '\ue93a'

    const val TextRotateVertical: Char = '\ue93b'

    const val TextRotationAngledown: Char = '\ue93c'

    const val TextRotationAngleup: Char = '\ue93d'

    const val TextRotationDown: Char = '\ue93e'

    const val TextRotationNone: Char = '\ue93f'

    const val Commute: Char = '\ue940'

    const val ArrowRightAlt: Char = '\ue941'

    const val WorkOff: Char = '\ue942'

    const val CollapseAll: Char = '\ue944'

    const val DragIndicator: Char = '\ue945'

    const val ExpandAll: Char = '\ue946'

    const val HorizontalSplit: Char = '\ue947'

    const val VerticalSplit: Char = '\ue949'

    const val VoiceOverOff: Char = '\ue94a'

    const val Segment: Char = '\ue94b'

    const val ContactSupport: Char = '\ue94c'

    const val Compress: Char = '\ue94d'

    const val FilterListAlt: Char = '\ue94e'

    const val Expand: Char = '\ue94f'

    const val EditOff: Char = '\ue950'

    const val _10k: Char = '\ue951'

    const val _10mp: Char = '\ue952'

    const val _11mp: Char = '\ue953'

    const val _12mp: Char = '\ue954'

    const val _13mp: Char = '\ue955'

    const val _14mp: Char = '\ue956'

    const val _15mp: Char = '\ue957'

    const val _16mp: Char = '\ue958'

    const val _17mp: Char = '\ue959'

    const val _18mp: Char = '\ue95a'

    const val _19mp: Char = '\ue95b'

    const val _1k: Char = '\ue95c'

    const val _1kPlus: Char = '\ue95d'

    const val _20mp: Char = '\ue95e'

    const val _21mp: Char = '\ue95f'

    const val _22mp: Char = '\ue960'

    const val _23mp: Char = '\ue961'

    const val _24mp: Char = '\ue962'

    const val _2k: Char = '\ue963'

    const val _2kPlus: Char = '\ue964'

    const val _2mp: Char = '\ue965'

    const val _3k: Char = '\ue966'

    const val _3kPlus: Char = '\ue967'

    const val _3mp: Char = '\ue968'

    const val _4kPlus: Char = '\ue969'

    const val _4mp: Char = '\ue96a'

    const val _5k: Char = '\ue96b'

    const val _5kPlus: Char = '\ue96c'

    const val _5mp: Char = '\ue96d'

    const val _6k: Char = '\ue96e'

    const val _6kPlus: Char = '\ue96f'

    const val _6mp: Char = '\ue970'

    const val _7k: Char = '\ue971'

    const val _7kPlus: Char = '\ue972'

    const val _7mp: Char = '\ue973'

    const val _8k: Char = '\ue974'

    const val _8kPlus: Char = '\ue975'

    const val _8mp: Char = '\ue976'

    const val _9k: Char = '\ue977'

    const val _9kPlus: Char = '\ue978'

    const val _9mp: Char = '\ue979'

    const val AccountTree: Char = '\ue97a'

    const val AddChart: Char = '\ue97b'

    const val AddModerator: Char = '\ue97d'

    const val AirPurifier: Char = '\ue97e'

    const val AllInbox: Char = '\ue97f'

    const val AppPromo: Char = '\ue981'

    const val Approval: Char = '\ue982'

    const val ArStickers: Char = '\ue983'

    const val ArrowDownwardAlt: Char = '\ue984'

    const val ArrowSplit: Char = '\ue985'

    const val ArrowUpwardAlt: Char = '\ue986'

    const val AssistantDevice: Char = '\ue987'

    const val AssistantDirection: Char = '\ue988'

    const val AssistantNavigation: Char = '\ue989'

    const val AutoDrawSolid: Char = '\ue98a'

    const val Bookmarks: Char = '\ue98b'

    const val BottomNavigation: Char = '\ue98c'

    const val BottomSheets: Char = '\ue98d'

    const val BrandAwareness: Char = '\ue98e'

    const val BusAlert: Char = '\ue98f'

    const val Cards: Char = '\ue991'

    const val Cases: Char = '\ue992'

    const val Chips: Char = '\ue993'

    const val CircleNotifications: Char = '\ue994'

    const val Cleaning: Char = '\ue995'

    const val Colors: Char = '\ue997'

    const val ConnectedTv: Char = '\ue998'

    const val ContactsProduct: Char = '\ue999'

    const val Dangerous: Char = '\ue99a'

    const val DashboardCustomize: Char = '\ue99b'

    const val DataTable: Char = '\ue99c'

    const val DesktopAccessDisabled: Char = '\ue99d'

    const val DeveloperGuide: Char = '\ue99e'

    const val Dialogs: Char = '\ue99f'

    const val Dishwasher: Char = '\ue9a0'

    const val DriveFileRenameOutline: Char = '\ue9a2'

    const val DriveFolderUpload: Char = '\ue9a3'

    const val Dropdown: Char = '\ue9a4'

    const val Duo: Char = '\ue9a5'

    const val Energy: Char = '\ue9a6'

    const val Spoke: Char = '\ue9a7'

    const val ExploreOff: Char = '\ue9a8'

    const val FeatureSearch: Char = '\ue9a9'

    const val DownloadDone: Char = '\ue9aa'

    const val FlightsAndHotels: Char = '\ue9ab'

    const val ForYou: Char = '\ue9ac'

    const val Rtt: Char = '\ue9ad'

    const val GridView: Char = '\ue9b0'

    const val Hail: Char = '\ue9b1'

    const val ImagesearchRoller: Char = '\ue9b4'

    const val JamboardKiosk: Char = '\ue9b5'

    const val LabelOff: Char = '\ue9b6'

    const val LibraryAddCheck: Char = '\ue9b7'

    const val LightOff: Char = '\ue9b8'

    const val Lists: Char = '\ue9b9'

    const val Logout: Char = '\ue9ba'

    const val Margin: Char = '\ue9bb'

    const val MarkAsUnread: Char = '\ue9bc'

    const val MenuOpen: Char = '\ue9bd'

    const val Mimo: Char = '\ue9be'

    const val MimoDisconnect: Char = '\ue9bf'

    const val MotionPhotosOff: Char = '\ue9c0'

    const val MotionPhotosOn: Char = '\ue9c1'

    const val MotionPhotosPaused: Char = '\ue9c2'

    const val Mp: Char = '\ue9c3'

    const val Newsstand: Char = '\ue9c4'

    const val OfflineShare: Char = '\ue9c5'

    const val Oven: Char = '\ue9c7'

    const val Padding: Char = '\ue9c8'

    const val PanoramaPhotosphere: Char = '\ue9c9'

    const val PersonAddDisabled: Char = '\ue9cb'

    const val PhoneDisabled: Char = '\ue9cc'

    const val PhoneEnabled: Char = '\ue9cd'

    const val PivotTableChart: Char = '\ue9ce'

    const val PrintDisabled: Char = '\ue9cf'

    const val ProgressActivity: Char = '\ue9d0'

    const val RailwayAlert: Char = '\ue9d1'

    const val Recommend: Char = '\ue9d2'

    const val RemoveDone: Char = '\ue9d3'

    const val RemoveModerator: Char = '\ue9d4'

    const val RemoveSelection: Char = '\ue9d5'

    const val RepeatOn: Char = '\ue9d6'

    const val RepeatOneOn: Char = '\ue9d7'

    const val ReplayCircleFilled: Char = '\ue9d8'

    const val ResetTv: Char = '\ue9d9'

    const val ResponsiveLayout: Char = '\ue9da'

    const val Ripples: Char = '\ue9db'

    const val ScrollableHeader: Char = '\ue9dc'

    const val Sd: Char = '\ue9dd'

    const val Shadow: Char = '\ue9df'

    const val Shield: Char = '\ue9e0'

    const val ShuffleOn: Char = '\ue9e1'

    const val SideNavigation: Char = '\ue9e2'

    const val Sliders: Char = '\ue9e3'

    const val Speed: Char = '\ue9e4'

    const val StackedBarChart: Char = '\ue9e6'

    const val Steppers: Char = '\ue9e7'

    const val StickyNote: Char = '\ue9e8'

    const val Stream: Char = '\ue9e9'

    const val Subheader: Char = '\ue9ea'

    const val Swipe: Char = '\ue9ec'

    const val SwitchAccount: Char = '\ue9ed'

    const val Tabs: Char = '\ue9ee'

    const val Tag: Char = '\ue9ef'

    const val TextFieldsAlt: Char = '\ue9f1'

    const val Hub: Char = '\ue9f4'

    const val ToggleOff: Char = '\ue9f5'

    const val ToggleOn: Char = '\ue9f6'

    const val Toolbar: Char = '\ue9f7'

    const val Tooltip: Char = '\ue9f8'

    const val TwoWheeler: Char = '\ue9f9'

    const val UniversalCurrency: Char = '\ue9fa'

    const val UniversalLocal: Char = '\ue9fb'

    const val UploadFile: Char = '\ue9fc'

    const val ViewInAr: Char = '\ue9fe'

    const val WaterfallChart: Char = '\uea00'

    const val WbShade: Char = '\uea01'

    const val WebTraffic: Char = '\uea03'

    const val BreakingNews: Char = '\uea08'

    const val HomeWork: Char = '\uea09'

    const val ScheduleSend: Char = '\uea0a'

    const val Bolt: Char = '\uea0b'

    const val SendAndArchive: Char = '\uea0c'

    const val FilePresent: Char = '\uea0e'

    const val FitScreen: Char = '\uea10'

    const val SavedSearch: Char = '\uea11'

    const val Storefront: Char = '\uea12'

    const val AmpStories: Char = '\uea13'

    const val DynamicFeed: Char = '\uea14'

    const val Euro: Char = '\uea15'

    const val Height: Char = '\uea16'

    const val Policy: Char = '\uea17'

    const val SyncAlt: Char = '\uea18'

    const val MenuBook: Char = '\uea19'

    const val EmojiFoodBeverage: Char = '\uea1b'

    const val EmojiNature: Char = '\uea1c'

    const val EmojiPeople: Char = '\uea1d'

    const val EmojiSymbols: Char = '\uea1e'

    const val EmojiTransportation: Char = '\uea1f'

    const val PostAdd: Char = '\uea20'

    const val EmojiObjects: Char = '\uea24'

    const val Token: Char = '\uea25'

    const val SportsBasketball: Char = '\uea26'

    const val SportsCricket: Char = '\uea27'

    const val SportsEsports: Char = '\uea28'

    const val SportsFootball: Char = '\uea29'

    const val SportsGolf: Char = '\uea2a'

    const val SportsHockey: Char = '\uea2b'

    const val SportsMma: Char = '\uea2c'

    const val SportsMotorsports: Char = '\uea2d'

    const val SportsRugby: Char = '\uea2e'

    const val SportsSoccer: Char = '\uea2f'

    const val Sports: Char = '\uea30'

    const val SportsVolleyball: Char = '\uea31'

    const val SportsTennis: Char = '\uea32'

    const val SportsHandball: Char = '\uea33'

    const val SportsKabaddi: Char = '\uea34'

    const val Eco: Char = '\uea35'

    const val Museum: Char = '\uea36'

    const val FlipCameraAndroid: Char = '\uea37'

    const val FlipCameraIos: Char = '\uea38'

    const val CancelScheduleSend: Char = '\uea39'

    const val Biotech: Char = '\uea3a'

    const val Architecture: Char = '\uea3b'

    const val Construction: Char = '\uea3c'

    const val Engineering: Char = '\uea3d'

    const val HistoryEdu: Char = '\uea3e'

    const val MilitaryTech: Char = '\uea3f'

    const val Apartment: Char = '\uea40'

    const val Bathtub: Char = '\uea41'

    const val Deck: Char = '\uea42'

    const val Fireplace: Char = '\uea43'

    const val House: Char = '\uea44'

    const val KingBed: Char = '\uea45'

    const val NightsStay: Char = '\uea46'

    const val OutdoorGrill: Char = '\uea47'

    const val SingleBed: Char = '\uea48'

    const val SquareFoot: Char = '\uea49'

    const val Psychology: Char = '\uea4a'

    const val Science: Char = '\uea4b'

    const val AutoDelete: Char = '\uea4c'

    const val CommentBank: Char = '\uea4e'

    const val Grading: Char = '\uea4f'

    const val DoubleArrow: Char = '\uea50'

    const val SportsBaseball: Char = '\uea51'

    const val Attractions: Char = '\uea52'

    const val BakeryDining: Char = '\uea53'

    const val BreakfastDining: Char = '\uea54'

    const val CarRental: Char = '\uea55'

    const val CarRepair: Char = '\uea56'

    const val DinnerDining: Char = '\uea57'

    const val DryCleaning: Char = '\uea58'

    const val Hardware: Char = '\uea59'

    const val Plagiarism: Char = '\uea5a'

    const val HourglassTop: Char = '\uea5b'

    const val HourglassBottom: Char = '\uea5c'

    const val MoreTime: Char = '\uea5d'

    const val AttachEmail: Char = '\uea5e'

    const val Calculate: Char = '\uea5f'

    const val Liquor: Char = '\uea60'

    const val LunchDining: Char = '\uea61'

    const val Nightlife: Char = '\uea62'

    const val Park: Char = '\uea63'

    const val RamenDining: Char = '\uea64'

    const val Celebration: Char = '\uea65'

    const val TheaterComedy: Char = '\uea66'

    const val Badge: Char = '\uea67'

    const val Festival: Char = '\uea68'

    const val Icecream: Char = '\uea69'

    const val VolunteerActivism: Char = '\uea70'

    const val Contactless: Char = '\uea71'

    const val Moped: Char = '\uea72'

    const val BrunchDining: Char = '\uea73'

    const val TakeoutDining: Char = '\uea74'

    const val VideoSettings: Char = '\uea75'

    const val SearchOff: Char = '\uea76'

    const val Login: Char = '\uea77'

    const val SelfImprovement: Char = '\uea78'

    const val Agriculture: Char = '\uea79'

    const val Docs: Char = '\uea7d'

    const val Files: Char = '\uea85'

    const val MedicationLiquid: Char = '\uea87'

    const val ContentPasteGo: Char = '\uea8e'

    const val Forest: Char = '\uea99'

    const val LineAxis: Char = '\uea9a'

    const val ContentPasteSearch: Char = '\uea9b'

    const val MonitorHeart: Char = '\ueaa2'

    const val Hive: Char = '\ueaa6'

    const val ArrowCircleLeft: Char = '\ueaa7'

    const val PunchClock: Char = '\ueaa8'

    const val ShieldMoon: Char = '\ueaa9'

    const val ArrowCircleRight: Char = '\ueaaa'

    const val Rotate90DegreesCw: Char = '\ueaab'

    const val Cookie: Char = '\ueaac'

    const val Fort: Char = '\ueaad'

    const val Church: Char = '\ueaae'

    const val TempleHindu: Char = '\ueaaf'

    const val Synagogue: Char = '\ueab0'

    const val Castle: Char = '\ueab1'

    const val Mosque: Char = '\ueab2'

    const val TempleBuddhist: Char = '\ueab3'

    const val UnknownMed: Char = '\ueabd'

    const val HeartBroken: Char = '\ueac2'

    const val KeyboardDoubleArrowLeft: Char = '\ueac3'

    const val TableRestaurant: Char = '\ueac6'

    const val Numbers: Char = '\ueac7'

    const val EggAlt: Char = '\ueac8'

    const val KeyboardDoubleArrowRight: Char = '\ueac9'

    const val InsertPageBreak: Char = '\ueaca'

    const val Egg: Char = '\ueacc'

    const val Route: Char = '\ueacd'

    const val KeyboardDoubleArrowUp: Char = '\ueacf'

    const val KeyboardDoubleArrowDown: Char = '\uead0'

    const val DataArray: Char = '\uead1'

    const val TableBar: Char = '\uead2'

    const val DataObject: Char = '\uead3'

    const val CandlestickChart: Char = '\uead4'

    const val Diamond: Char = '\uead5'

    const val LogoDev: Char = '\uead6'

    const val Phishing: Char = '\uead7'

    const val Fax: Char = '\uead8'

    const val WifiTetheringError: Char = '\uead9'

    const val AdfScanner: Char = '\ueada'

    const val SendTimeExtension: Char = '\ueadb'

    const val TextDecrease: Char = '\ueadd'

    const val LockReset: Char = '\ueade'

    const val TextIncrease: Char = '\ueae2'

    const val WatchOff: Char = '\ueae3'

    const val AppShortcut: Char = '\ueae4'

    const val AdGroupOff: Char = '\ueae5'

    const val KeyboardControlKey: Char = '\ueae6'

    const val KeyboardCommandKey: Char = '\ueae7'

    const val KeyboardOptionKey: Char = '\ueae8'

    const val SportsMartialArts: Char = '\ueae9'

    const val JoinRight: Char = '\ueaea'

    const val Join: Char = '\ueaeb'

    const val CurrencyRuble: Char = '\ueaec'

    const val ThreatIntelligence: Char = '\ueaed'

    const val SyncLock: Char = '\ueaee'

    const val CurrencyLira: Char = '\ueaef'

    const val CoPresent: Char = '\ueaf0'

    const val CurrencyPound: Char = '\ueaf1'

    const val JoinLeft: Char = '\ueaf2'

    const val FileOpen: Char = '\ueaf3'

    const val JoinInner: Char = '\ueaf4'

    const val Commit: Char = '\ueaf5'

    const val Balance: Char = '\ueaf6'

    const val CurrencyRupee: Char = '\ueaf7'

    const val FlagCircle: Char = '\ueaf8'

    const val CurrencyYuan: Char = '\ueaf9'

    const val CurrencyFranc: Char = '\ueafa'

    const val CurrencyYen: Char = '\ueafb'

    const val DrawingRecognition: Char = '\ueb00'

    const val ShapeRecognition: Char = '\ueb01'

    const val HandwritingRecognition: Char = '\ueb02'

    const val LassoSelect: Char = '\ueb03'

    const val License: Char = '\ueb04'

    const val Unlicense: Char = '\ueb05'

    const val CarryOnBag: Char = '\ueb08'

    const val CarryOnBagQuestion: Char = '\ueb09'

    const val CarryOnBagInactive: Char = '\ueb0a'

    const val CarryOnBagChecked: Char = '\ueb0b'

    const val CheckedBag: Char = '\ueb0c'

    const val CheckedBagQuestion: Char = '\ueb0d'

    const val PersonalBag: Char = '\ueb0e'

    const val PersonalBagOff: Char = '\ueb0f'

    const val PersonalBagQuestion: Char = '\ueb10'

    const val FullCoverage: Char = '\ueb12'

    const val Browse: Char = '\ueb13'

    const val Orders: Char = '\ueb14'

    const val QuickReorder: Char = '\ueb15'

    const val OutboxAlt: Char = '\ueb17'

    const val Crowdsource: Char = '\ueb18'

    const val FamilyLink: Char = '\ueb19'

    const val MusicCast: Char = '\ueb1a'

    const val ElectricBike: Char = '\ueb1b'

    const val ElectricCar: Char = '\ueb1c'

    const val ElectricMoped: Char = '\ueb1d'

    const val ElectricRickshaw: Char = '\ueb1e'

    const val ElectricScooter: Char = '\ueb1f'

    const val FamilyHome: Char = '\ueb26'

    const val Rubric: Char = '\ueb27'

    const val PedalBike: Char = '\ueb29'

    const val ThingsToDo: Char = '\ueb2a'

    const val YourTrips: Char = '\ueb2b'

    const val FolderZip: Char = '\ueb2c'

    const val ZoomInMap: Char = '\ueb2d'

    const val SwipeUp: Char = '\ueb2e'

    const val Lan: Char = '\ueb2f'

    const val SwipeDownAlt: Char = '\ueb30'

    const val WifiFind: Char = '\ueb31'

    const val FilterAltOff: Char = '\ueb32'

    const val SwipeLeftAlt: Char = '\ueb33'

    const val FolderDelete: Char = '\ueb34'

    const val SwipeUpAlt: Char = '\ueb35'

    const val Square: Char = '\ueb36'

    const val Contrast: Char = '\ueb37'

    const val Pinch: Char = '\ueb38'

    const val Hexagon: Char = '\ueb39'

    const val SatelliteAlt: Char = '\ueb3a'

    const val AcUnit: Char = '\ueb3b'

    const val AirportShuttle: Char = '\ueb3c'

    const val AllInclusive: Char = '\ueb3d'

    const val BeachAccess: Char = '\ueb3e'

    const val BusinessCenter: Char = '\ueb3f'

    const val Casino: Char = '\ueb40'

    const val ChildCare: Char = '\ueb41'

    const val ChildFriendly: Char = '\ueb42'

    const val FitnessCenter: Char = '\ueb43'

    const val GolfCourse: Char = '\ueb45'

    const val HotTub: Char = '\ueb46'

    const val Kitchen: Char = '\ueb47'

    const val Pool: Char = '\ueb48'

    const val RoomService: Char = '\ueb49'

    const val SmokeFree: Char = '\ueb4a'

    const val SmokingRooms: Char = '\ueb4b'

    const val Spa: Char = '\ueb4c'

    const val EnterpriseOff: Char = '\ueb4d'

    const val NoMeetingRoom: Char = '\ueb4e'

    const val MeetingRoom: Char = '\ueb4f'

    const val Pentagon: Char = '\ueb50'

    const val SwipeVertical: Char = '\ueb51'

    const val SwipeRight: Char = '\ueb52'

    const val SwipeDown: Char = '\ueb53'

    const val Rectangle: Char = '\ueb54'

    const val SwipeRightAlt: Char = '\ueb56'

    const val FilterListOff: Char = '\ueb57'

    const val Percent: Char = '\ueb58'

    const val SwipeLeft: Char = '\ueb59'

    const val CloudSync: Char = '\ueb5a'

    const val TrailLength: Char = '\ueb5e'

    const val Scale: Char = '\ueb5f'

    const val SaveAs: Char = '\ueb60'

    const val MoveDown: Char = '\ueb61'

    const val DomainAdd: Char = '\ueb62'

    const val TrailLengthMedium: Char = '\ueb63'

    const val MoveUp: Char = '\ueb64'

    const val FormatOverline: Char = '\ueb65'

    const val SsidChart: Char = '\ueb66'

    const val Boy: Char = '\ueb67'

    const val Girl: Char = '\ueb68'

    const val ElderlyWoman: Char = '\ueb69'

    const val WifiChannel: Char = '\ueb6a'

    const val WifiPassword: Char = '\ueb6b'

    const val TrailLengthShort: Char = '\ueb6d'

    const val AssuredWorkload: Char = '\ueb6f'

    const val CurrencyExchange: Char = '\ueb70'

    const val InstallDesktop: Char = '\ueb71'

    const val InstallMobile: Char = '\ueb72'

    const val ViewComfyAlt: Char = '\ueb73'

    const val ViewCompactAlt: Char = '\ueb74'

    const val ViewCozy: Char = '\ueb75'

    const val BedtimeOff: Char = '\ueb76'

    const val Deblur: Char = '\ueb77'

    const val VpnKeyOff: Char = '\ueb7a'

    const val EventRepeat: Char = '\ueb7b'

    const val Javascript: Char = '\ueb7c'

    const val Difference: Char = '\ueb7d'

    const val Html: Char = '\ueb7e'

    const val ViewKanban: Char = '\ueb7f'

    const val PlaylistRemove: Char = '\ueb80'

    const val Newspaper: Char = '\ueb81'

    const val AudioFile: Char = '\ueb82'

    const val FolderOff: Char = '\ueb83'

    const val KeyOff: Char = '\ueb84'

    const val ViewTimeline: Char = '\ueb85'

    const val AddCard: Char = '\ueb86'

    const val VideoFile: Char = '\ueb87'

    const val ShoppingCartCheckout: Char = '\ueb88'

    const val Hls: Char = '\ueb8a'

    const val QuestionMark: Char = '\ueb8b'

    const val HlsOff: Char = '\ueb8c'

    const val _123: Char = '\ueb8d'

    const val Terminal: Char = '\ueb8e'

    const val Php: Char = '\ueb8f'

    const val Stadium: Char = '\ueb90'

    const val Signpost: Char = '\ueb91'

    const val Webhook: Char = '\ueb92'

    const val Css: Char = '\ueb93'

    const val Abc: Char = '\ueb94'

    const val Straight: Char = '\ueb95'

    const val RampRight: Char = '\ueb96'

    const val DisplaySettings: Char = '\ueb97'

    const val Merge: Char = '\ueb98'

    const val RoundaboutLeft: Char = '\ueb99'

    const val TurnSlightRight: Char = '\ueb9a'

    const val RocketLaunch: Char = '\ueb9b'

    const val RampLeft: Char = '\ueb9c'

    const val MarkUnreadChatAlt: Char = '\ueb9d'

    const val DensityMedium: Char = '\ueb9e'

    const val DataThresholding: Char = '\ueb9f'

    const val ForkLeft: Char = '\ueba0'

    const val UTurnLeft: Char = '\ueba1'

    const val UTurnRight: Char = '\ueba2'

    const val RoundaboutRight: Char = '\ueba3'

    const val TurnSlightLeft: Char = '\ueba4'

    const val Rocket: Char = '\ueba5'

    const val TurnLeft: Char = '\ueba6'

    const val TurnSharpLeft: Char = '\ueba7'

    const val DensitySmall: Char = '\ueba8'

    const val DensityLarge: Char = '\ueba9'

    const val TurnSharpRight: Char = '\uebaa'

    const val TurnRight: Char = '\uebab'

    const val ForkRight: Char = '\uebac'

    const val Chronic: Char = '\uebb2'

    const val Deselect: Char = '\uebb6'

    const val IdentityPlatform: Char = '\uebb7'

    const val Warehouse: Char = '\uebb8'

    const val PanToolAlt: Char = '\uebb9'

    const val CellTower: Char = '\uebba'

    const val Polyline: Char = '\uebbb'

    const val Factory: Char = '\uebbc'

    const val FolderCopy: Char = '\uebbd'

    const val Output: Char = '\uebbe'

    const val NestAudio: Char = '\uebbf'

    const val SportsGymnastics: Char = '\uebc4'

    const val CurrencyBitcoin: Char = '\uebc5'

    const val VapeFree: Char = '\uebc6'

    const val Atr: Char = '\uebc7'

    const val TireRepair: Char = '\uebc8'

    const val NetworkPing: Char = '\uebca'

    const val Handshake: Char = '\uebcb'

    const val CalendarMonth: Char = '\uebcc'

    const val RollerSkating: Char = '\uebcd'

    const val ScubaDiving: Char = '\uebce'

    const val VapingRooms: Char = '\uebcf'

    const val Scoreboard: Char = '\uebd0'

    const val BrowseGallery: Char = '\uebd1'

    const val Battery6Bar: Char = '\uebd2'

    const val SevereCold: Char = '\uebd3'

    const val Battery5Bar: Char = '\uebd4'

    const val Cyclone: Char = '\uebd5'

    const val NetworkWifi2Bar: Char = '\uebd6'

    const val Landslide: Char = '\uebd7'

    const val Tsunami: Char = '\uebd8'

    const val Battery1Bar: Char = '\uebd9'

    const val Volcano: Char = '\uebda'

    const val Thunderstorm: Char = '\uebdb'

    const val Battery0Bar: Char = '\uebdc'

    const val Battery3Bar: Char = '\uebdd'

    const val DevicesFold: Char = '\uebde'

    const val SignalCellularAlt1Bar: Char = '\uebdf'

    const val Battery2Bar: Char = '\uebe0'

    const val NetworkWifi3Bar: Char = '\uebe1'

    const val Battery4Bar: Char = '\uebe2'

    const val SignalCellularAlt2Bar: Char = '\uebe3'

    const val NetworkWifi1Bar: Char = '\uebe4'

    const val SignLanguage: Char = '\uebe5'

    const val Flood: Char = '\uebe6'

    const val ManageHistory: Char = '\uebe7'

    const val SpatialAudioOff: Char = '\uebe8'

    const val CrisisAlert: Char = '\uebe9'

    const val SpatialTracking: Char = '\uebea'

    const val SpatialAudio: Char = '\uebeb'

    const val NoiseAware: Char = '\uebec'

    const val MedicalInformation: Char = '\uebed'

    const val ScreenRotationAlt: Char = '\uebee'

    const val SafetyCheck: Char = '\uebef'

    const val NoCrash: Char = '\uebf0'

    const val MinorCrash: Char = '\uebf1'

    const val CarCrash: Char = '\uebf2'

    const val NoiseControlOff: Char = '\uebf3'

    const val EmergencyRecording: Char = '\uebf4'

    const val EmergencyShare: Char = '\uebf6'

    const val Sos: Char = '\uebf7'

    const val RemoveRoad: Char = '\uebfc'

    const val OnDeviceTraining: Char = '\uebfd'

    const val LightbulbCircle: Char = '\uebfe'

    const val Hourglass: Char = '\uebff'

    const val ScreenshotMonitor: Char = '\uec08'

    const val WorkHistory: Char = '\uec09'

    const val MailLock: Char = '\uec0a'

    const val Lyrics: Char = '\uec0b'

    const val WindPower: Char = '\uec0c'

    const val VerticalShadesClosed: Char = '\uec0d'

    const val VerticalShades: Char = '\uec0e'

    const val SolarPower: Char = '\uec0f'

    const val SensorOccupied: Char = '\uec10'

    const val RollerShadesClosed: Char = '\uec11'

    const val RollerShades: Char = '\uec12'

    const val PropaneTank: Char = '\uec13'

    const val Propane: Char = '\uec14'

    const val OilBarrel: Char = '\uec15'

    const val NestCamWiredStand: Char = '\uec16'

    const val ModeFanOff: Char = '\uec17'

    const val HeatPump: Char = '\uec18'

    const val GasMeter: Char = '\uec19'

    const val EnergySavingsLeaf: Char = '\uec1a'

    const val ElectricMeter: Char = '\uec1b'

    const val ElectricBolt: Char = '\uec1c'

    const val CurtainsClosed: Char = '\uec1d'

    const val Curtains: Char = '\uec1e'

    const val BlindsClosed: Char = '\uec1f'

    const val AutoMode: Char = '\uec20'

    const val StarRateHalf: Char = '\uec45'

    const val ContrastRtlOff: Char = '\uec72'

    const val KeyboardTabRtl: Char = '\uec73'

    const val _2d: Char = '\uef37'

    const val _5g: Char = '\uef38'

    const val AdUnits: Char = '\uef39'

    const val AddLocationAlt: Char = '\uef3a'

    const val AddRoad: Char = '\uef3b'

    const val AdminPanelSettings: Char = '\uef3d'

    const val Analytics: Char = '\uef3e'

    const val AppBlocking: Char = '\uef3f'

    const val AppRegistration: Char = '\uef40'

    const val Article: Char = '\uef42'

    const val BackupTable: Char = '\uef43'

    const val Bedtime: Char = '\uef44'

    const val BikeScooter: Char = '\uef45'

    const val BuildCircle: Char = '\uef48'

    const val Campaign: Char = '\uef49'

    const val Circle: Char = '\uef4a'

    const val DirtyLens: Char = '\uef4b'

    const val DomainVerification: Char = '\uef4c'

    const val EditRoad: Char = '\uef4d'

    const val FaceRetouchingNatural: Char = '\uef4e'

    const val FilterAlt: Char = '\uef4f'

    const val Flaky: Char = '\uef50'

    const val HdrEnhancedSelect: Char = '\uef51'

    const val HourglassDisabled: Char = '\uef53'

    const val IntegrationInstructions: Char = '\uef54'

    const val LocalFireDepartment: Char = '\uef55'

    const val LocalPolice: Char = '\uef56'

    const val LockClock: Char = '\uef57'

    const val MapsUgc: Char = '\uef58'

    const val MicExternalOff: Char = '\uef59'

    const val MicExternalOn: Char = '\uef5a'

    const val Monitor: Char = '\uef5b'

    const val Nat: Char = '\uef5c'

    const val NextPlan: Char = '\uef5d'

    const val Nightlight: Char = '\uef5e'

    const val Outbox: Char = '\uef5f'

    const val Payments: Char = '\uef63'

    const val Pending: Char = '\uef64'

    const val PersonRemove: Char = '\uef66'

    const val PhotoCameraBack: Char = '\uef68'

    const val PhotoCameraFront: Char = '\uef69'

    const val PlayDisabled: Char = '\uef6a'

    const val QrCode: Char = '\uef6b'

    const val Quickreply: Char = '\uef6c'

    const val ReadMore: Char = '\uef6d'

    const val ReceiptLong: Char = '\uef6e'

    const val RunCircle: Char = '\uef6f'

    const val ScreenSearchDesktop: Char = '\uef70'

    const val StopCircle: Char = '\uef71'

    const val SubtitlesOff: Char = '\uef72'

    const val Support: Char = '\uef73'

    const val TaxiAlert: Char = '\uef74'

    const val Tour: Char = '\uef75'

    const val WifiCalling: Char = '\uef77'

    const val WrongLocation: Char = '\uef78'

    const val Apparel: Char = '\uef7b'

    const val ArOnYou: Char = '\uef7c'

    const val ArrowLeftAlt: Char = '\uef7d'

    const val AutoTimer: Char = '\uef7f'

    const val BidLandscapeDisabled: Char = '\uef81'

    const val BooksMoviesAndMusic: Char = '\uef82'

    const val Bubble: Char = '\uef83'

    const val BusinessMessages: Char = '\uef84'

    const val CalendarAddOn: Char = '\uef85'

    const val DigitalWellbeing: Char = '\uef86'

    const val EvShadow: Char = '\uef8f'

    const val FeaturedSeasonalAndGifts: Char = '\uef91'

    const val FinanceMode: Char = '\uef92'

    const val Grocery: Char = '\uef97'

    const val HandGesture: Char = '\uef9c'

    const val HealthAndBeauty: Char = '\uef9d'

    const val Hide: Char = '\uef9e'

    const val HomeAndGarden: Char = '\uef9f'

    const val HomeImprovementAndTools: Char = '\uefa0'

    const val HouseholdSupplies: Char = '\uefa1'

    const val Imagesmode: Char = '\uefa2'

    const val LiftToTalk: Char = '\uefa3'

    const val LightningStand: Char = '\uefa4'

    const val Mediation: Char = '\uefa7'

    const val Mintmark: Char = '\uefa9'

    const val MultipleAirports: Char = '\uefab'

    const val NetworkIntelligence: Char = '\uefac'

    const val Newsmode: Char = '\uefad'

    const val OpenJam: Char = '\uefae'

    const val PartnerReports: Char = '\uefaf'

    const val PetSupplies: Char = '\uefb1'

    const val PhotoPrints: Char = '\uefb2'

    const val RewardedAds: Char = '\uefb6'

    const val Shoppingmode: Char = '\uefb7'

    const val SportsAndOutdoors: Char = '\uefb8'

    const val Timer10Alt1: Char = '\uefbf'

    const val Timer3Alt1: Char = '\uefc0'

    const val Toast: Char = '\uefc1'

    const val ToysAndGames: Char = '\uefc2'

    const val TravelLuggageAndBags: Char = '\uefc3'

    const val Vacuum: Char = '\uefc5'

    const val VideoSearch: Char = '\uefc6'

    const val Vr180Create2d: Char = '\uefca'

    const val WallArt: Char = '\uefcb'

    const val _1xMobiledata: Char = '\uefcd'

    const val _30fps: Char = '\uefce'

    const val _30fpsSelect: Char = '\uefcf'

    const val _3gMobiledata: Char = '\uefd0'

    const val _3p: Char = '\uefd1'

    const val _4gMobiledata: Char = '\uefd2'

    const val _4gPlusMobiledata: Char = '\uefd3'

    const val _60fps: Char = '\uefd4'

    const val _60fpsSelect: Char = '\uefd5'

    const val Air: Char = '\uefd8'

    const val AirplaneTicket: Char = '\uefd9'

    const val Aod: Char = '\uefda'

    const val Attribution: Char = '\uefdb'

    const val AutofpsSelect: Char = '\uefdc'

    const val Bathroom: Char = '\uefdd'

    const val BatterySaver: Char = '\uefde'

    const val Bed: Char = '\uefdf'

    const val BedroomBaby: Char = '\uefe0'

    const val BedroomChild: Char = '\uefe1'

    const val BedroomParent: Char = '\uefe2'

    const val Blender: Char = '\uefe3'

    const val Bloodtype: Char = '\uefe4'

    const val BluetoothDrive: Char = '\uefe5'

    const val Cable: Char = '\uefe6'

    const val CalendarViewMonth: Char = '\uefe7'

    const val CalendarViewWeek: Char = '\uefe8'

    const val CameraIndoor: Char = '\uefe9'

    const val CameraOutdoor: Char = '\uefea'

    const val Cameraswitch: Char = '\uefeb'

    const val CastForEducation: Char = '\uefec'

    const val Chair: Char = '\uefed'

    const val ChairAlt: Char = '\uefee'

    const val Coffee: Char = '\uefef'

    const val CoffeeMaker: Char = '\ueff0'

    const val CreditScore: Char = '\ueff1'

    const val DataSaverOn: Char = '\ueff3'

    const val Dining: Char = '\ueff4'

    const val DoNotDisturbOnTotalSilence: Char = '\ueffb'

    const val DoorBack: Char = '\ueffc'

    const val DoorFront: Char = '\ueffd'

    const val DoorSliding: Char = '\ueffe'

    const val Doorbell: Char = '\uefff'

    const val DownloadForOffline: Char = '\uf000'

    const val Downloading: Char = '\uf001'

    const val EMobiledata: Char = '\uf002'

    const val Earbuds: Char = '\uf003'

    const val EarbudsBattery: Char = '\uf004'

    const val EdgesensorHigh: Char = '\uf005'

    const val EdgesensorLow: Char = '\uf006'

    const val FaceRetouchingOff: Char = '\uf007'

    const val Feed: Char = '\uf009'

    const val FlashlightOff: Char = '\uf00a'

    const val FlashlightOn: Char = '\uf00b'

    const val Flatware: Char = '\uf00c'

    const val FmdBad: Char = '\uf00e'

    const val GMobiledata: Char = '\uf010'

    const val Garage: Char = '\uf011'

    const val GppBad: Char = '\uf012'

    const val GppMaybe: Char = '\uf014'

    const val Grid3x3: Char = '\uf015'

    const val Grid4x4: Char = '\uf016'

    const val GridGoldenratio: Char = '\uf017'

    const val HMobiledata: Char = '\uf018'

    const val HPlusMobiledata: Char = '\uf019'

    const val HdrAuto: Char = '\uf01a'

    const val HdrAutoSelect: Char = '\uf01b'

    const val HdrOffSelect: Char = '\uf01c'

    const val HdrOnSelect: Char = '\uf01d'

    const val HdrPlus: Char = '\uf01e'

    const val HeadphonesBattery: Char = '\uf020'

    const val Hevc: Char = '\uf021'

    const val HideImage: Char = '\uf022'

    const val HideSource: Char = '\uf023'

    const val HomeMax: Char = '\uf024'

    const val HomeMini: Char = '\uf025'

    const val KeyboardAlt: Char = '\uf028'

    const val LensBlur: Char = '\uf029'

    const val Light: Char = '\uf02a'

    const val Living: Char = '\uf02b'

    const val LteMobiledata: Char = '\uf02c'

    const val LtePlusMobiledata: Char = '\uf02d'

    const val ManageAccounts: Char = '\uf02e'

    const val ManageSearch: Char = '\uf02f'

    const val MediaBluetoothOff: Char = '\uf031'

    const val MediaBluetoothOn: Char = '\uf032'

    const val Medication: Char = '\uf033'

    const val MobiledataOff: Char = '\uf034'

    const val ModeStandby: Char = '\uf037'

    const val MonitorWeight: Char = '\uf039'

    const val MotionPhotosAuto: Char = '\uf03a'

    const val NearbyError: Char = '\uf03b'

    const val NearbyOff: Char = '\uf03c'

    const val NoAccounts: Char = '\uf03e'

    const val NoteAlt: Char = '\uf040'

    const val Paid: Char = '\uf041'

    const val Password: Char = '\uf042'

    const val Pattern: Char = '\uf043'

    const val Pin: Char = '\uf045'

    const val PlayLesson: Char = '\uf047'

    const val Podcasts: Char = '\uf048'

    const val PrecisionManufacturing: Char = '\uf049'

    const val PriceChange: Char = '\uf04a'

    const val PriceCheck: Char = '\uf04b'

    const val Quiz: Char = '\uf04c'

    const val RMobiledata: Char = '\uf04d'

    const val Radar: Char = '\uf04e'

    const val RawOff: Char = '\uf04f'

    const val RawOn: Char = '\uf050'

    const val RememberMe: Char = '\uf051'

    const val RestartAlt: Char = '\uf053'

    const val Reviews: Char = '\uf054'

    const val Rsvp: Char = '\uf055'

    const val Screenshot: Char = '\uf056'

    const val SecurityUpdateGood: Char = '\uf059'

    const val SecurityUpdateWarning: Char = '\uf05a'

    const val SendToMobile: Char = '\uf05c'

    const val SettingsAccessibility: Char = '\uf05d'

    const val SettingsSuggest: Char = '\uf05e'

    const val ShareLocation: Char = '\uf05f'

    const val Shower: Char = '\uf061'

    const val SignalCellularNodata: Char = '\uf062'

    const val SignalWifiBad: Char = '\uf063'

    const val SignalWifiStatusbarNull: Char = '\uf067'

    const val SimCardDownload: Char = '\uf068'

    const val Sip: Char = '\uf069'

    const val SmartDisplay: Char = '\uf06a'

    const val SmartScreen: Char = '\uf06b'

    const val SmartToy: Char = '\uf06c'

    const val Splitscreen: Char = '\uf06d'

    const val SportsScore: Char = '\uf06e'

    const val Storm: Char = '\uf070'

    const val Summarize: Char = '\uf071'

    const val Task: Char = '\uf075'

    const val Thermostat: Char = '\uf076'

    const val ThermostatAuto: Char = '\uf077'

    const val Timer10Select: Char = '\uf07a'

    const val Timer3Select: Char = '\uf07b'

    const val Upcoming: Char = '\uf07e'

    const val VideoCameraBack: Char = '\uf07f'

    const val VideoCameraFront: Char = '\uf080'

    const val VideoStable: Char = '\uf081'

    const val Vrpano: Char = '\uf082'

    const val Water: Char = '\uf084'

    const val WifiCalling1: Char = '\uf085'

    const val Window: Char = '\uf088'

    const val Yard: Char = '\uf089'

    const val Cut: Char = '\uf08b'

    const val Insights: Char = '\uf092'

    const val BatteryCharging20: Char = '\uf0a2'

    const val BatteryCharging30: Char = '\uf0a3'

    const val BatteryCharging50: Char = '\uf0a4'

    const val BatteryCharging60: Char = '\uf0a5'

    const val BatteryCharging80: Char = '\uf0a6'

    const val BatteryCharging90: Char = '\uf0a7'

    const val SignalCellular0Bar: Char = '\uf0a8'

    const val SignalCellular1Bar: Char = '\uf0a9'

    const val SignalCellular2Bar: Char = '\uf0aa'

    const val SignalCellular3Bar: Char = '\uf0ab'

    const val SignalCellularConnectedNoInternet0Bar: Char = '\uf0ac'

    const val SignalWifi0Bar: Char = '\uf0b0'

    const val BreakingNewsAlt1: Char = '\uf0ba'

    const val CalendarAppsScript: Char = '\uf0bb'

    const val ChatAppsScript: Char = '\uf0bd'

    const val Clarify: Char = '\uf0bf'

    const val ConversionPath: Char = '\uf0c1'

    const val DocsAddOn: Char = '\uf0c2'

    const val DocsAppsScript: Char = '\uf0c3'

    const val FactCheck: Char = '\uf0c5'

    const val FormsAddOn: Char = '\uf0c7'

    const val FormsAppsScript: Char = '\uf0c8'

    const val ModelTraining: Char = '\uf0cf'

    const val MotionBlur: Char = '\uf0d0'

    const val NotStarted: Char = '\uf0d1'

    const val OutgoingMail: Char = '\uf0d2'

    const val PhotoFrame: Char = '\uf0d9'

    const val PrivacyTip: Char = '\uf0dc'

    const val SupportAgent: Char = '\uf0e2'

    const val Tenancy: Char = '\uf0e3'

    const val TimeAuto: Char = '\uf0e4'

    const val OnlinePrediction: Char = '\uf0eb'

    const val StarRate: Char = '\uf0ec'

    const val SignalWifiStatusbarNotConnected: Char = '\uf0ef'

    const val ChatAddOn: Char = '\uf0f3'

    const val BatchPrediction: Char = '\uf0f5'

    const val WifiCalling2: Char = '\uf0f6'

    const val PestControl: Char = '\uf0fa'

    const val Upgrade: Char = '\uf0fb'

    const val WifiProtectedSetup: Char = '\uf0fc'

    const val PestControlRodent: Char = '\uf0fd'

    const val NotAccessible: Char = '\uf0fe'

    const val CleaningServices: Char = '\uf0ff'

    const val HomeRepairService: Char = '\uf100'

    const val TableRows: Char = '\uf101'

    const val ElectricalServices: Char = '\uf102'

    const val HearingDisabled: Char = '\uf104'

    const val PersonSearch: Char = '\uf106'

    const val Plumbing: Char = '\uf107'

    const val HorizontalRule: Char = '\uf108'

    const val MedicalServices: Char = '\uf109'

    const val DesignServices: Char = '\uf10a'

    const val Handyman: Char = '\uf10b'

    const val PushPin: Char = '\uf10d'

    const val Hvac: Char = '\uf10e'

    const val DirectionsOff: Char = '\uf10f'

    const val Subscript: Char = '\uf111'

    const val Superscript: Char = '\uf112'

    const val ViewSidebar: Char = '\uf114'

    const val ImageNotSupported: Char = '\uf116'

    const val E911Emergency: Char = '\uf119'

    const val E911Avatar: Char = '\uf11a'

    const val LegendToggle: Char = '\uf11b'

    const val HomeSpeaker: Char = '\uf11c'

    const val MfgNestYaleLock: Char = '\uf11d'

    const val NestCamIndoor: Char = '\uf11e'

    const val NestCamIq: Char = '\uf11f'

    const val NestCamIqOutdoor: Char = '\uf120'

    const val NestCamOutdoor: Char = '\uf121'

    const val NestConnect: Char = '\uf122'

    const val NestDetect: Char = '\uf123'

    const val NestDisplay: Char = '\uf124'

    const val NestDisplayMax: Char = '\uf125'

    const val NestHeatLinkE: Char = '\uf126'

    const val NestHeatLinkGen3: Char = '\uf127'

    const val GoogleTvRemote: Char = '\uf129'

    const val NestRemoteComfortSensor: Char = '\uf12a'

    const val NestSecureAlarm: Char = '\uf12b'

    const val NestThermostatEEu: Char = '\uf12d'

    const val NestThermostatGen3: Char = '\uf12e'

    const val NestThermostatSensor: Char = '\uf12f'

    const val NestThermostatSensorEu: Char = '\uf130'

    const val NestThermostatZirconiumEu: Char = '\uf131'

    const val NestWifiGale: Char = '\uf132'

    const val StadiaController: Char = '\uf135'

    const val MagicButton: Char = '\uf136'

    const val PlayPause: Char = '\uf137'

    const val BatteryFullAlt: Char = '\uf13b'

    const val SettingsAlert: Char = '\uf143'

    const val BatteryVeryLow: Char = '\uf147'

    const val Privacy: Char = '\uf148'

    const val SoundDetectionDogBarking: Char = '\uf149'

    const val SoundDetectionGlassBreak: Char = '\uf14a'

    const val SoundDetectionLoudSound: Char = '\uf14b'

    const val HomePin: Char = '\uf14d'

    const val LocationAutomation: Char = '\uf14f'

    const val LocationAway: Char = '\uf150'

    const val LocationHome: Char = '\uf152'

    const val BatteryLow: Char = '\uf155'

    const val ClearDay: Char = '\uf157'

    const val ClearNight: Char = '\uf159'

    const val EmergencyHeat: Char = '\uf15d'

    const val EnergyProgramSaving: Char = '\uf15f'

    const val EnergyProgramTimeUsed: Char = '\uf161'

    const val HumidityHigh: Char = '\uf163'

    const val HumidityLow: Char = '\uf164'

    const val HumidityMid: Char = '\uf165'

    const val ModeCool: Char = '\uf166'

    const val ModeCoolOff: Char = '\uf167'

    const val ModeFan: Char = '\uf168'

    const val ModeHeat: Char = '\uf16a'

    const val ModeHeatCool: Char = '\uf16b'

    const val ModeHeatOff: Char = '\uf16d'

    const val ModeOffOn: Char = '\uf16f'

    const val PartlyCloudyDay: Char = '\uf172'

    const val PartlyCloudyNight: Char = '\uf174'

    const val Rainy: Char = '\uf176'

    const val ThermostatCarbon: Char = '\uf178'

    const val Chromecast2: Char = '\uf17b'

    const val HistoryToggleOff: Char = '\uf17d'

    const val PointOfSale: Char = '\uf17e'

    const val FileSave: Char = '\uf17f'

    const val ArrowCircleDown: Char = '\uf181'

    const val ArrowCircleUp: Char = '\uf182'

    const val AltRoute: Char = '\uf184'

    const val ForwardToInbox: Char = '\uf187'

    const val Enable: Char = '\uf188'

    const val MarkChatUnread: Char = '\uf189'

    const val MarkEmailUnread: Char = '\uf18a'

    const val MarkChatRead: Char = '\uf18b'

    const val MarkEmailRead: Char = '\uf18c'

    const val Monitoring: Char = '\uf190'

    const val Table: Char = '\uf191'

    const val SentimentExtremelyDissatisfied: Char = '\uf194'

    const val MoreDown: Char = '\uf196'

    const val MoreUp: Char = '\uf197'

    const val KeyVisualizer: Char = '\uf199'

    const val BabyChangingStation: Char = '\uf19b'

    const val Backpack: Char = '\uf19c'

    const val ChargingStation: Char = '\uf19d'

    const val Checkroom: Char = '\uf19e'

    const val DoNotStep: Char = '\uf19f'

    const val Elevator: Char = '\uf1a0'

    const val Escalator: Char = '\uf1a1'

    const val FamilyRestroom: Char = '\uf1a2'

    const val FireHydrant: Char = '\uf1a3'

    const val NoDrinks: Char = '\uf1a5'

    const val NoFlash: Char = '\uf1a6'

    const val NoFood: Char = '\uf1a7'

    const val NoPhotography: Char = '\uf1a8'

    const val Stairs: Char = '\uf1a9'

    const val Tty: Char = '\uf1aa'

    const val WheelchairPickup: Char = '\uf1ab'

    const val EscalatorWarning: Char = '\uf1ac'

    const val Umbrella: Char = '\uf1ad'

    const val Stroller: Char = '\uf1ae'

    const val NoStroller: Char = '\uf1af'

    const val DoNotTouch: Char = '\uf1b0'

    const val Wash: Char = '\uf1b1'

    const val Soap: Char = '\uf1b2'

    const val Dry: Char = '\uf1b3'

    const val SensorWindow: Char = '\uf1b4'

    const val SensorDoor: Char = '\uf1b5'

    const val RequestQuote: Char = '\uf1b6'

    const val Api: Char = '\uf1b7'

    const val RoomPreferences: Char = '\uf1b8'

    const val MultipleStop: Char = '\uf1b9'

    const val PendingActions: Char = '\uf1bb'

    const val TextToSpeech: Char = '\uf1bc'

    const val TableView: Char = '\uf1be'

    const val DynamicForm: Char = '\uf1bf'

    const val HelpCenter: Char = '\uf1c0'

    const val SmartButton: Char = '\uf1c1'

    const val Rule: Char = '\uf1c2'

    const val Wysiwyg: Char = '\uf1c3'

    const val Topic: Char = '\uf1c4'

    const val Preview: Char = '\uf1c5'

    const val TextSnippet: Char = '\uf1c6'

    const val SnippetFolder: Char = '\uf1c7'

    const val RuleFolder: Char = '\uf1c9'

    const val PublicOff: Char = '\uf1ca'

    const val ShoppingBag: Char = '\uf1cc'

    const val Anchor: Char = '\uf1cd'

    const val OpenInFull: Char = '\uf1ce'

    const val CloseFullscreen: Char = '\uf1cf'

    const val CorporateFare: Char = '\uf1d0'

    const val SwitchLeft: Char = '\uf1d1'

    const val SwitchRight: Char = '\uf1d2'

    const val Outlet: Char = '\uf1d4'

    const val NoTransfer: Char = '\uf1d5'

    const val NoMeals: Char = '\uf1d6'

    const val NightSightAuto: Char = '\uf1d7'

    const val FireExtinguisher: Char = '\uf1d8'

    const val AstrophotographyAuto: Char = '\uf1d9'

    const val AstrophotographyOff: Char = '\uf1da'

    const val ClosedCaptionDisabled: Char = '\uf1dc'

    const val Flutter: Char = '\uf1dd'

    const val DigitalOutOfHome: Char = '\uf1de'

    const val East: Char = '\uf1df'

    const val North: Char = '\uf1e0'

    const val NorthEast: Char = '\uf1e1'

    const val NorthWest: Char = '\uf1e2'

    const val South: Char = '\uf1e3'

    const val SouthEast: Char = '\uf1e4'

    const val SouthWest: Char = '\uf1e5'

    const val West: Char = '\uf1e6'

    const val ComponentExchange: Char = '\uf1e7'

    const val WineBar: Char = '\uf1e8'

    const val Tapas: Char = '\uf1e9'

    const val SetMeal: Char = '\uf1ea'

    const val NearMeDisabled: Char = '\uf1ef'

    const val PlaceItem: Char = '\uf1f0'

    const val NightShelter: Char = '\uf1f1'

    const val FoodBank: Char = '\uf1f2'

    const val SportsBar: Char = '\uf1f3'

    const val Bento: Char = '\uf1f4'

    const val RiceBowl: Char = '\uf1f5'

    const val Fence: Char = '\uf1f6'

    const val Countertops: Char = '\uf1f7'

    const val Carpenter: Char = '\uf1f8'

    const val NightSightAutoOff: Char = '\uf1f9'

    const val PinchZoomIn: Char = '\uf1fa'

    const val PinchZoomOut: Char = '\uf1fb'

    const val StickyNote2: Char = '\uf1fc'

    const val SelectCheckBox: Char = '\uf1fe'

    const val MoveItem: Char = '\uf1ff'

    const val Foundation: Char = '\uf200'

    const val Roofing: Char = '\uf201'

    const val HouseSiding: Char = '\uf202'

    const val WaterDamage: Char = '\uf203'

    const val Microwave: Char = '\uf204'

    const val Grass: Char = '\uf205'

    const val QrCodeScanner: Char = '\uf206'

    const val BackgroundReplace: Char = '\uf20a'

    const val Leaderboard: Char = '\uf20c'

    const val Database: Char = '\uf20e'

    const val GroupedBarChart: Char = '\uf211'

    const val FullStackedBarChart: Char = '\uf212'

    const val AutoReadPlay: Char = '\uf216'

    const val BookOnline: Char = '\uf217'

    const val Masks: Char = '\uf218'

    const val AutoReadPause: Char = '\uf219'

    const val Elderly: Char = '\uf21a'

    const val ReduceCapacity: Char = '\uf21c'

    const val Sanitizer: Char = '\uf21d'

    const val _6FtApart: Char = '\uf21e'

    const val CleanHands: Char = '\uf21f'

    const val Sick: Char = '\uf220'

    const val Coronavirus: Char = '\uf221'

    const val FollowTheSigns: Char = '\uf222'

    const val ConnectWithoutContact: Char = '\uf223'

    const val StackedLineChart: Char = '\uf22b'

    const val RequestPage: Char = '\uf22c'

    const val ContactPage: Char = '\uf22e'

    const val Exclamation: Char = '\uf22f'

    const val DisabledByDefault: Char = '\uf230'

    const val PublishedWithChanges: Char = '\uf232'

    const val Groups: Char = '\uf233'

    const val Luggage: Char = '\uf235'

    const val Unpublished: Char = '\uf236'

    const val NoBackpack: Char = '\uf237'

    const val EventUpcoming: Char = '\uf238'

    const val SignalDisconnected: Char = '\uf239'

    const val AddTask: Char = '\uf23a'

    const val NoLuggage: Char = '\uf23b'

    const val FileExport: Char = '\uf3b2'

    const val SquareDot: Char = '\uf3b3'

    const val Owl: Char = '\uf3b4'

    const val Cognition2: Char = '\uf3b5'

    const val ChessPawn: Char = '\uf3b6'

    const val WidgetWidth: Char = '\uf3b8'

    const val WidgetSmall: Char = '\uf3b9'

    const val WidgetMedium: Char = '\uf3ba'

    const val FileJson: Char = '\uf3bb'

    const val FilePng: Char = '\uf3bc'

    const val ServerPerson: Char = '\uf3bd'

    const val DesktopCloudStack: Char = '\uf3be'

    const val SplitScene: Char = '\uf3bf'

    const val TileSmall: Char = '\uf3c1'

    const val TileMedium: Char = '\uf3c2'

    const val TileLarge: Char = '\uf3c3'

    const val TwoPagerStore: Char = '\uf3c4'

    const val TextCompare: Char = '\uf3c5'

    const val TableEdit: Char = '\uf3c6'

    const val TableConvert: Char = '\uf3c7'

    const val FolderCode: Char = '\uf3c8'

    const val GlobeBook: Char = '\uf3c9'

    const val MapSearch: Char = '\uf3ca'

    const val ChatPasteGo2: Char = '\uf3cb'

    const val CloudAlert: Char = '\uf3cc'

    const val LaptopCar: Char = '\uf3cd'

    const val GroupSearch: Char = '\uf3ce'

    const val UpiPay: Char = '\uf3cf'

    const val TabCloseInactive: Char = '\uf3d0'

    const val FilterArrowRight: Char = '\uf3d1'

    const val ArrowMenuOpen: Char = '\uf3d2'

    const val ArrowMenuClose: Char = '\uf3d3'

    const val FolderMatch: Char = '\uf3d4'

    const val FolderEye: Char = '\uf3d5'

    const val FolderCheck2: Char = '\uf3d6'

    const val FolderCheck: Char = '\uf3d7'

    const val FlagCheck: Char = '\uf3d8'

    const val Host: Char = '\uf3d9'

    const val HardDisk: Char = '\uf3da'

    const val DesktopCloud: Char = '\uf3db'

    const val DatabaseUpload: Char = '\uf3dc'

    const val Add2: Char = '\uf3dd'

    const val ListAltCheck: Char = '\uf3de'

    const val Book6: Char = '\uf3df'

    const val Book4Spark: Char = '\uf3e0'

    const val Simulation: Char = '\uf3e1'

    const val FileMapStack: Char = '\uf3e2'

    const val Lightbulb2: Char = '\uf3e3'

    const val ForkSpoon: Char = '\uf3e4'

    const val SearchActivity: Char = '\uf3e5'

    const val History2: Char = '\uf3e6'

    const val BookRibbon: Char = '\uf3e7'

    const val Dashboard2: Char = '\uf3ea'

    const val TvNext: Char = '\uf3eb'

    const val TvDisplays: Char = '\uf3ec'

    const val Tooltip2: Char = '\uf3ed'

    const val MoneyBag: Char = '\uf3ee'

    const val TransitTicket: Char = '\uf3f1'

    const val _24fpsSelect: Char = '\uf3f2'

    const val HandGestureOff: Char = '\uf3f3'

    const val ArrowUploadProgress: Char = '\uf3f4'

    const val ArrowUploadReady: Char = '\uf3f5'

    const val EraserSize5: Char = '\uf3f8'

    const val EraserSize4: Char = '\uf3f9'

    const val EraserSize3: Char = '\uf3fa'

    const val EraserSize2: Char = '\uf3fb'

    const val EraserSize1: Char = '\uf3fc'

    const val FaceUp: Char = '\uf3fd'

    const val FaceShake: Char = '\uf3fe'

    const val FaceRight: Char = '\uf3ff'

    const val FaceNod: Char = '\uf400'

    const val FaceLeft: Char = '\uf401'

    const val FaceDown: Char = '\uf402'

    const val DevicesFold2: Char = '\uf406'

    const val PolicyAlert: Char = '\uf407'

    const val TrackpadInput3: Char = '\uf408'

    const val TrackpadInput2: Char = '\uf409'

    const val ReceiptLongOff: Char = '\uf40a'

    const val MotionPlay: Char = '\uf40b'

    const val VideoCameraBackAdd: Char = '\uf40c'

    const val Borg: Char = '\uf40d'

    const val Gif2: Char = '\uf40e'

    const val Flag2: Char = '\uf40f'

    const val BookmarkBag: Char = '\uf410'

    const val BarChartOff: Char = '\uf411'

    const val FormatQuoteOff: Char = '\uf413'

    const val DatabaseOff: Char = '\uf414'

    const val RotateAuto: Char = '\uf417'

    const val PowerSettingsCircle: Char = '\uf418'

    const val SyncDesktop: Char = '\uf41a'

    const val MultimodalHandEye: Char = '\uf41b'

    const val StackHexagon: Char = '\uf41c'

    const val DriveExport: Char = '\uf41d'

    const val DiagonalLine: Char = '\uf41e'

    const val ConvertToText: Char = '\uf41f'

    const val CombineColumns: Char = '\uf420'

    const val Automation: Char = '\uf421'

    const val AddRowBelow: Char = '\uf422'

    const val AddRowAbove: Char = '\uf423'

    const val AddColumnRight: Char = '\uf424'

    const val AddColumnLeft: Char = '\uf425'

    const val Orbit: Char = '\uf426'

    const val EncryptedOff: Char = '\uf427'

    const val EncryptedMinusCircle: Char = '\uf428'

    const val EncryptedAdd: Char = '\uf429'

    const val EncryptedAddCircle: Char = '\uf42a'

    const val MaskedTransitionsAdd: Char = '\uf42b'

    const val VoiceSelectionOff: Char = '\uf42c'

    const val EditAudio: Char = '\uf42d'

    const val ViewObjectTrack: Char = '\uf432'

    const val CategorySearch: Char = '\uf437'

    const val CreditCardClock: Char = '\uf438'

    const val DesktopLandscapeAdd: Char = '\uf439'

    const val ArrowBack2: Char = '\uf43a'

    const val TabInactive: Char = '\uf43b'

    const val WifiCallingBar3: Char = '\uf44a'

    const val WifiCallingBar2: Char = '\uf44b'

    const val WifiCallingBar1: Char = '\uf44c'

    const val TabletCamera: Char = '\uf44d'

    const val SmartphoneCamera: Char = '\uf44e'

    const val ReplaceVideo: Char = '\uf44f'

    const val ReplaceImage: Char = '\uf450'

    const val ReplaceAudio: Char = '\uf451'

    const val BookmarkStar: Char = '\uf454'

    const val BookmarkHeart: Char = '\uf455'

    const val BookmarkFlag: Char = '\uf456'

    const val BookmarkCheck: Char = '\uf457'

    const val SplitscreenPortrait: Char = '\uf458'

    const val SplitscreenLandscape: Char = '\uf459'

    const val FullscreenPortrait: Char = '\uf45a'

    const val FloatPortrait2: Char = '\uf45b'

    const val FloatLandscape2: Char = '\uf45c'

    const val DesktopPortrait: Char = '\uf45d'

    const val DesktopLandscape: Char = '\uf45e'

    const val Script: Char = '\uf45f'

    const val CurrencyRupeeCircle: Char = '\uf460'

    const val RailwayAlert2: Char = '\uf461'

    const val DirectionsRailway2: Char = '\uf462'

    const val FitnessTracker: Char = '\uf463'

    const val HearingAid: Char = '\uf464'

    const val TableEye: Char = '\uf466'

    const val WatchVibration: Char = '\uf467'

    const val WatchCheck: Char = '\uf468'

    const val SearchCheck2: Char = '\uf469'

    const val ChevronForward: Char = '\uf46a'

    const val ChevronBackward: Char = '\uf46b'

    const val Stairs2: Char = '\uf46c'

    const val UnpavedRoad: Char = '\uf46d'

    const val TrolleyCableCar: Char = '\uf46e'

    const val TrafficJam: Char = '\uf46f'

    const val SpeedCamera: Char = '\uf470'

    const val Scooter: Char = '\uf471'

    const val Road: Char = '\uf472'

    const val Monorail: Char = '\uf473'

    const val Metro: Char = '\uf474'

    const val Hov: Char = '\uf475'

    const val GondolaLift: Char = '\uf476'

    const val Funicular: Char = '\uf477'

    const val Flyover: Char = '\uf478'

    const val CableCar: Char = '\uf479'

    const val BikeLane: Char = '\uf47a'

    const val BikeDock: Char = '\uf47b'

    const val ResetWhiteBalance: Char = '\uf47c'

    const val ResetShutterSpeed: Char = '\uf47d'

    const val ResetShadow: Char = '\uf47e'

    const val ResetSettings: Char = '\uf47f'

    const val ResetIso: Char = '\uf480'

    const val ResetFocus: Char = '\uf481'

    const val ResetBrightness: Char = '\uf482'

    const val ShiftLockOff: Char = '\uf483'

    const val ContextualTokenAdd: Char = '\uf485'

    const val ContextualToken: Char = '\uf486'

    const val Uppercase: Char = '\uf488'

    const val Titlecase: Char = '\uf489'

    const val Lowercase: Char = '\uf48a'

    const val MailOff: Char = '\uf48b'

    const val AddTriangle: Char = '\uf48e'

    const val MouseLockOff: Char = '\uf48f'

    const val MouseLock: Char = '\uf490'

    const val KeyboardLockOff: Char = '\uf491'

    const val KeyboardLock: Char = '\uf492'

    const val Speed17x: Char = '\uf493'

    const val Speed15x: Char = '\uf494'

    const val Speed12x: Char = '\uf495'

    const val Speed07x: Char = '\uf496'

    const val Speed05x: Char = '\uf497'

    const val Speed02x: Char = '\uf498'

    const val MovieOff: Char = '\uf499'

    const val AnimatedImages: Char = '\uf49a'

    const val PokerChip: Char = '\uf49b'

    const val AddDiamond: Char = '\uf49c'

    const val FingerprintOff: Char = '\uf49d'

    const val ContrastSquare: Char = '\uf4a0'

    const val SmartCardReader: Char = '\uf4a5'

    const val SmartCardReaderOff: Char = '\uf4a6'

    const val Password2Off: Char = '\uf4a8'

    const val Password2: Char = '\uf4a9'

    const val Vo2Max: Char = '\uf4aa'

    const val SlabSerif: Char = '\uf4ab'

    const val Serif: Char = '\uf4ac'

    const val ClosedCaptionAdd: Char = '\uf4ae'

    const val Avc: Char = '\uf4af'

    const val Av1: Char = '\uf4b0'

    const val Timer5: Char = '\uf4b1'

    const val Timer5Shutter: Char = '\uf4b2'

    const val Cadence: Char = '\uf4b4'

    const val ArrowWarmUp: Char = '\uf4b5'

    const val ArrowCoolDown: Char = '\uf4b6'

    const val OpenRun: Char = '\uf4b7'

    const val FormatTextdirectionVertical: Char = '\uf4b8'

    const val CardioLoad: Char = '\uf4b9'

    const val TimerPlay: Char = '\uf4ba'

    const val TimerPause: Char = '\uf4bb'

    const val SearchInsights: Char = '\uf4bc'

    const val Recenter: Char = '\uf4c0'

    const val Guardian: Char = '\uf4c1'

    const val ViewRealSize: Char = '\uf4c2'

    const val Landscape2Off: Char = '\uf4c3'

    const val Landscape2: Char = '\uf4c4'

    const val HeadMountedDevice: Char = '\uf4c5'

    const val HandheldController: Char = '\uf4c6'

    const val TrackpadInput: Char = '\uf4c7'

    const val SelectWindow2: Char = '\uf4c8'

    const val EyeTracking: Char = '\uf4c9'

    const val IdCard: Char = '\uf4ca'

    const val AdaptiveAudioMicOff: Char = '\uf4cb'

    const val AdaptiveAudioMic: Char = '\uf4cc'

    const val EmojiLanguage: Char = '\uf4cd'

    const val SpatialSpeaker: Char = '\uf4cf'

    const val OfflinePinOff: Char = '\uf4d0'

    const val Speed175: Char = '\uf4d1'

    const val Speed125: Char = '\uf4d2'

    const val Speed075: Char = '\uf4d3'

    const val Speed025: Char = '\uf4d4'

    const val FramePersonMic: Char = '\uf4d5'

    const val ComedyMask: Char = '\uf4d6'

    const val FileCopyOff: Char = '\uf4d8'

    const val AttachFileOff: Char = '\uf4d9'

    const val HistoryOff: Char = '\uf4da'

    const val Speed15: Char = '\uf4e0'

    const val Speed12: Char = '\uf4e1'

    const val Speed05: Char = '\uf4e2'

    const val CarTag: Char = '\uf4e3'

    const val FolderLimited: Char = '\uf4e4'

    const val EmergencyHeat2: Char = '\uf4e5'

    const val TouchpadMouseOff: Char = '\uf4e6'

    const val Speed2x: Char = '\uf4eb'

    const val BacklightHighOff: Char = '\uf4ef'

    const val BrandFamily: Char = '\uf4f1'

    const val MediaOutput: Char = '\uf4f2'

    const val MediaOutputOff: Char = '\uf4f3'

    const val PromptSuggestion: Char = '\uf4f6'

    const val ShoppingCartOff: Char = '\uf4f7'

    const val ThreadUnread: Char = '\uf4f9'

    const val PersonEdit: Char = '\uf4fa'

    const val SplitscreenVerticalAdd: Char = '\uf4fc'

    const val SplitscreenAdd: Char = '\uf4fd'

    const val NotificationsUnread: Char = '\uf4fe'

    const val Stacks: Char = '\uf500'

    const val PulseAlert: Char = '\uf501'

    const val ActionKey: Char = '\uf502'

    const val SecurityKey: Char = '\uf503'

    const val SwitchAccess2: Char = '\uf506'

    const val CollapseContent: Char = '\uf507'

    const val CloseSmall: Char = '\uf508'

    const val Pageless: Char = '\uf509'

    const val TransitionSlide: Char = '\uf50a'

    const val TransitionPush: Char = '\uf50b'

    const val TransitionFade: Char = '\uf50c'

    const val TransitionDissolve: Char = '\uf50d'

    const val TransitionChop: Char = '\uf50e'

    const val HighlightKeyboardFocus: Char = '\uf510'

    const val HighlightMouseCursor: Char = '\uf511'

    const val HighlightTextCursor: Char = '\uf512'

    const val LanguageJapaneseKana: Char = '\uf513'

    const val BackgroundDotSmall: Char = '\uf514'

    const val SensorsKrxOff: Char = '\uf515'

    const val PictureInPictureMobile: Char = '\uf517'

    const val DeleteHistory: Char = '\uf518'

    const val KeyVertical: Char = '\uf51a'

    const val DeployedCodeAccount: Char = '\uf51b'

    const val VariableRemove: Char = '\uf51c'

    const val VariableInsert: Char = '\uf51d'

    const val VariableAdd: Char = '\uf51e'

    const val TwoPager: Char = '\uf51f'

    const val Upload2: Char = '\uf521'

    const val SettingsHeart: Char = '\uf522'

    const val Download2: Char = '\uf523'

    const val InkHighlighterMove: Char = '\uf524'

    const val Asterisk: Char = '\uf525'

    const val KidStar: Char = '\uf526'

    const val FamilyStar: Char = '\uf527'

    const val EditorChoice: Char = '\uf528'

    const val ShieldQuestion: Char = '\uf529'

    const val P2p: Char = '\uf52a'

    const val ChatInfo: Char = '\uf52b'

    const val CreditCardHeart: Char = '\uf52c'

    const val CreditCardGear: Char = '\uf52d'

    const val PictureInPictureOff: Char = '\uf52f'

    const val PhotoAutoMerge: Char = '\uf530'

    const val NetworkWifiLocked: Char = '\uf532'

    const val LinkedServices: Char = '\uf535'

    const val Heat: Char = '\uf537'

    const val Dictionary: Char = '\uf539'

    const val Book5: Char = '\uf53b'

    const val Book4: Char = '\uf53c'

    const val Book3: Char = '\uf53d'

    const val Book2: Char = '\uf53e'

    const val AutoTransmission: Char = '\uf53f'

    const val CalendarClock: Char = '\uf540'

    const val ScienceOff: Char = '\uf542'

    const val Skillet: Char = '\uf543'

    const val SkilletCooktop: Char = '\uf544'

    const val Stockpot: Char = '\uf545'

    const val Mitre: Char = '\uf547'

    const val Crop916: Char = '\uf549'

    const val NotAccessibleForward: Char = '\uf54a'

    const val HighRes: Char = '\uf54b'

    const val PictureInPictureSmall: Char = '\uf54d'

    const val PictureInPictureMedium: Char = '\uf54e'

    const val PictureInPictureLarge: Char = '\uf54f'

    const val PictureInPictureCenter: Char = '\uf550'

    const val Markdown: Char = '\uf552'

    const val MarkdownCopy: Char = '\uf553'

    const val MarkdownPaste: Char = '\uf554'

    const val Raven: Char = '\uf555'

    const val SensorsKrx: Char = '\uf556'

    const val ModeDual: Char = '\uf557'

    const val HumidityIndoor: Char = '\uf558'

    const val FarsightDigital: Char = '\uf559'

    const val Aq: Char = '\uf55a'

    const val AqIndoor: Char = '\uf55b'

    const val RadioButtonPartial: Char = '\uf560'

    const val Concierge: Char = '\uf561'

    const val NoteStack: Char = '\uf562'

    const val NoteStackAdd: Char = '\uf563'

    const val Tactic: Char = '\uf564'

    const val PersonCheck: Char = '\uf565'

    const val PersonCancel: Char = '\uf566'

    const val PersonAlert: Char = '\uf567'

    const val Bomb: Char = '\uf568'

    const val Package2: Char = '\uf569'

    const val NestWifiPro2: Char = '\uf56a'

    const val NestWifiPro: Char = '\uf56b'

    const val ResetWrench: Char = '\uf56c'

    const val IndeterminateQuestionBox: Char = '\uf56d'

    const val NetworkNode: Char = '\uf56e'

    const val KeepPublic: Char = '\uf56f'

    const val StockMedia: Char = '\uf570'

    const val Vr180Create2dOff: Char = '\uf571'

    const val TextureMinus: Char = '\uf57b'

    const val TextureAdd: Char = '\uf57c'

    const val ShutterSpeedMinus: Char = '\uf57d'

    const val ShutterSpeedAdd: Char = '\uf57e'

    const val EvShadowMinus: Char = '\uf57f'

    const val EvShadowAdd: Char = '\uf580'

    const val ThermometerMinus: Char = '\uf581'

    const val ThermometerAdd: Char = '\uf582'

    const val ShadowMinus: Char = '\uf583'

    const val ShadowAdd: Char = '\uf584'

    const val Destruction: Char = '\uf585'

    const val FolderData: Char = '\uf586'

    const val ArticleShortcut: Char = '\uf587'

    const val Candle: Char = '\uf588'

    const val VoiceSelection: Char = '\uf58a'

    const val FullHd: Char = '\uf58b'

    const val AudioDescription: Char = '\uf58c'

    const val NetworkWifi3BarLocked: Char = '\uf58d'

    const val NetworkWifi2BarLocked: Char = '\uf58e'

    const val NetworkWifi1BarLocked: Char = '\uf58f'

    const val ExpandCircleRight: Char = '\uf591'

    const val ShieldLocked: Char = '\uf592'

    const val PersonRaisedHand: Char = '\uf59a'

    const val InfoI: Char = '\uf59b'

    const val SafetyCheckOff: Char = '\uf59d'

    const val EmergencyShareOff: Char = '\uf59e'

    const val Contract: Char = '\uf5a0'

    const val ContractEdit: Char = '\uf5a1'

    const val ContractDelete: Char = '\uf5a2'

    const val PersonApron: Char = '\uf5a3'

    const val Box: Char = '\uf5a4'

    const val BoxAdd: Char = '\uf5a5'

    const val BoxEdit: Char = '\uf5a6'

    const val SignalCellularPause: Char = '\uf5a7'

    const val BrightnessAlert: Char = '\uf5cf'

    const val Robot2: Char = '\uf5d0'

    const val MicDouble: Char = '\uf5d1'

    const val ExpandCircleUp: Char = '\uf5d2'

    const val AudioVideoReceiver: Char = '\uf5d3'

    const val ArrowOrEdge: Char = '\uf5d6'

    const val ArrowAndEdge: Char = '\uf5d7'

    const val WaterPump: Char = '\uf5d8'

    const val TvRemote: Char = '\uf5d9'

    const val PlayingCards: Char = '\uf5dc'

    const val ComicBubble: Char = '\uf5dd'

    const val SwordRose: Char = '\uf5de'

    const val Strategy: Char = '\uf5df'

    const val Mystery: Char = '\uf5e1'

    const val MountainFlag: Char = '\uf5e2'

    const val Manga: Char = '\uf5e3'

    const val DominoMask: Char = '\uf5e4'

    const val Crossword: Char = '\uf5e5'

    const val Chess: Char = '\uf5e7'

    const val PersonBook: Char = '\uf5e8'

    const val LanguageSpanish: Char = '\uf5e9'

    const val FoldedHands: Char = '\uf5ed'

    const val Joystick: Char = '\uf5ee'

    const val CastWarning: Char = '\uf5ef'

    const val CastPause: Char = '\uf5f0'

    const val DeployedCodeAlert: Char = '\uf5f2'

    const val DeployedCodeHistory: Char = '\uf5f3'

    const val DeployedCodeUpdate: Char = '\uf5f4'

    const val NetworkIntelligenceUpdate: Char = '\uf5f5'

    const val NetworkIntelligenceHistory: Char = '\uf5f6'

    const val WorkAlert: Char = '\uf5f7'

    const val WorkUpdate: Char = '\uf5f8'

    const val StylusNote: Char = '\uf603'

    const val Stylus: Char = '\uf604'

    const val StackStar: Char = '\uf607'

    const val StackOff: Char = '\uf608'

    const val Stack: Char = '\uf609'

    const val HeartCheck: Char = '\uf60a'

    const val WeatherMix: Char = '\uf60b'

    const val Helicopter: Char = '\uf60c'

    const val Falling: Char = '\uf60d'

    const val SupervisedUserCircleOff: Char = '\uf60e'

    const val AwardStar: Char = '\uf612'

    const val ShareWindows: Char = '\uf613'

    const val PageInfo: Char = '\uf614'

    const val FormatLetterSpacingWider: Char = '\uf615'

    const val FormatLetterSpacingWide: Char = '\uf616'

    const val FormatLetterSpacingStandard: Char = '\uf617'

    const val FormatLetterSpacing2: Char = '\uf618'

    const val ViewInArOff: Char = '\uf61b'

    const val SnowingHeavy: Char = '\uf61c'

    const val RainySnow: Char = '\uf61d'

    const val RainyLight: Char = '\uf61e'

    const val RainyHeavy: Char = '\uf61f'

    const val SettingsVideoCamera: Char = '\uf621'

    const val SettingsTimelapse: Char = '\uf622'

    const val SettingsSlowMotion: Char = '\uf623'

    const val SettingsCinematicBlur: Char = '\uf624'

    const val SettingsBRoll: Char = '\uf625'

    const val RuleSettings: Char = '\uf64c'

    const val Pip: Char = '\uf64d'

    const val Bubbles: Char = '\uf64e'

    const val Earthquake: Char = '\uf64f'

    const val ShieldPerson: Char = '\uf650'

    const val PrintLock: Char = '\uf651'

    const val CallQuality: Char = '\uf652'

    const val VisibilityLock: Char = '\uf653'

    const val ReleaseAlert: Char = '\uf654'

    const val PanZoom: Char = '\uf655'

    const val LockOpenRight: Char = '\uf656'

    const val GestureSelect: Char = '\uf657'

    const val QrCode2Add: Char = '\uf658'

    const val WifiNotification: Char = '\uf670'

    const val WifiHome: Char = '\uf671'

    const val WallpaperSlideshow: Char = '\uf672'

    const val SplitscreenTop: Char = '\uf673'

    const val SplitscreenRight: Char = '\uf674'

    const val SplitscreenLeft: Char = '\uf675'

    const val SplitscreenBottom: Char = '\uf676'

    const val ScreenshotFrame: Char = '\uf677'

    const val ScreenRotationUp: Char = '\uf678'

    const val ScreenRecord: Char = '\uf679'

    const val KeyboardOff: Char = '\uf67a'

    const val KeyboardKeys: Char = '\uf67b'

    const val Grid3x3Off: Char = '\uf67c'

    const val BatteryStatusGood: Char = '\uf67d'

    const val BatteryShare: Char = '\uf67e'

    const val WeatherHail: Char = '\uf67f'

    const val BarChart4Bars: Char = '\uf681'

    const val Autostop: Char = '\uf682'

    const val EventList: Char = '\uf683'

    const val BottomRightClick: Char = '\uf684'

    const val Explosion: Char = '\uf685'

    const val ShieldLock: Char = '\uf686'

    const val TouchpadMouse: Char = '\uf687'

    const val ScreenshotTablet: Char = '\uf697'

    const val ArrowRange: Char = '\uf69b'

    const val Wrist: Char = '\uf69c'

    const val WaterBottle: Char = '\uf69d'

    const val WaterBottleLarge: Char = '\uf69e'

    const val Taunt: Char = '\uf69f'

    const val SocialLeaderboard: Char = '\uf6a0'

    const val SentimentWorried: Char = '\uf6a1'

    const val SentimentStressed: Char = '\uf6a2'

    const val SentimentSad: Char = '\uf6a3'

    const val SentimentFrustrated: Char = '\uf6a4'

    const val SentimentExcited: Char = '\uf6a5'

    const val SentimentContent: Char = '\uf6a6'

    const val SentimentCalm: Char = '\uf6a7'

    const val Cheer: Char = '\uf6a8'

    const val WatchWake: Char = '\uf6a9'

    const val WatchButtonPress: Char = '\uf6aa'

    const val DevicesWearables: Char = '\uf6ab'

    const val AodWatch: Char = '\uf6ac'

    const val WaterLock: Char = '\uf6ad'

    const val WatchScreentime: Char = '\uf6ae'

    const val MeasuringTape: Char = '\uf6af'

    const val AlarmSmartWake: Char = '\uf6b0'

    const val SoundSampler: Char = '\uf6b4'

    const val Autoplay: Char = '\uf6b5'

    const val Autopause: Char = '\uf6b6'

    const val SleepScore: Char = '\uf6b7'

    const val Pace: Char = '\uf6b8'

    const val Laps: Char = '\uf6b9'

    const val HrResting: Char = '\uf6ba'

    const val AvgPace: Char = '\uf6bb'

    const val ChatPasteGo: Char = '\uf6bd'

    const val AutoLabel: Char = '\uf6be'

    const val AutoMeetingRoom: Char = '\uf6bf'

    const val AutoVideocam: Char = '\uf6c0'

    const val AssistantOnHub: Char = '\uf6c1'

    const val RearCamera: Char = '\uf6c2'

    const val NightSightMax: Char = '\uf6c3'

    const val AmbientScreen: Char = '\uf6c4'

    const val ShareOff: Char = '\uf6cb'

    const val VpnKeyAlert: Char = '\uf6cc'

    const val DualScreen: Char = '\uf6cf'

    const val WaterMedium: Char = '\uf6d4'

    const val WaterLoss: Char = '\uf6d5'

    const val WaterFull: Char = '\uf6d6'

    const val ThermometerLoss: Char = '\uf6d7'

    const val ThermometerGain: Char = '\uf6d8'

    const val StressManagement: Char = '\uf6d9'

    const val Steps: Char = '\uf6da'

    const val Spo2: Char = '\uf6db'

    const val Relax: Char = '\uf6dc'

    const val ReadinessScore: Char = '\uf6dd'

    const val MonitorWeightLoss: Char = '\uf6de'

    const val MonitorWeightGain: Char = '\uf6df'

    const val Mindfulness: Char = '\uf6e0'

    const val MenstrualHealth: Char = '\uf6e1'

    const val HealthMetrics: Char = '\uf6e2'

    const val GlassCup: Char = '\uf6e3'

    const val Floor: Char = '\uf6e4'

    const val Fertile: Char = '\uf6e5'

    const val Exercise: Char = '\uf6e6'

    const val Elevation: Char = '\uf6e7'

    const val Eda: Char = '\uf6e8'

    const val EcgHeart: Char = '\uf6e9'

    const val Distance: Char = '\uf6ea'

    const val Bia: Char = '\uf6eb'

    const val Azm: Char = '\uf6ec'

    const val Eyeglasses: Char = '\uf6ee'

    const val TableChartView: Char = '\uf6ef'

    const val MatchWord: Char = '\uf6f0'

    const val MatchCase: Char = '\uf6f1'

    const val MacroAuto: Char = '\uf6f2'

    const val _50mp: Char = '\uf6f3'

    const val ForwardMedia: Char = '\uf6f4'

    const val ForwardCircle: Char = '\uf6f5'

    const val CheckInOut: Char = '\uf6f6'

    const val Sauna: Char = '\uf6f7'

    const val Onsen: Char = '\uf6f8'

    const val BathPublicLarge: Char = '\uf6f9'

    const val BathPrivate: Char = '\uf6fa'

    const val BathOutdoor: Char = '\uf6fb'

    const val SwitchAccess: Char = '\uf6fd'

    const val Step: Char = '\uf6fe'

    const val StepOver: Char = '\uf6ff'

    const val StepOut: Char = '\uf700'

    const val StepInto: Char = '\uf701'

    const val ShelfPosition: Char = '\uf702'

    const val ShelfAutoHide: Char = '\uf703'

    const val RightPanelOpen: Char = '\uf704'

    const val RightPanelClose: Char = '\uf705'

    const val RightClick: Char = '\uf706'

    const val Resize: Char = '\uf707'

    const val ReopenWindow: Char = '\uf708'

    const val PositionTopRight: Char = '\uf709'

    const val PositionBottomRight: Char = '\uf70a'

    const val PositionBottomLeft: Char = '\uf70b'

    const val PointScan: Char = '\uf70c'

    const val PipExit: Char = '\uf70d'

    const val OutputCircle: Char = '\uf70e'

    const val OpenInNewDown: Char = '\uf70f'

    const val NewWindow: Char = '\uf710'

    const val MoveSelectionUp: Char = '\uf711'

    const val MoveSelectionRight: Char = '\uf712'

    const val MoveSelectionLeft: Char = '\uf713'

    const val MoveSelectionDown: Char = '\uf714'

    const val MoveGroup: Char = '\uf715'

    const val LeftPanelOpen: Char = '\uf716'

    const val LeftPanelClose: Char = '\uf717'

    const val LeftClick: Char = '\uf718'

    const val JumpToElement: Char = '\uf719'

    const val InputCircle: Char = '\uf71a'

    const val Iframe: Char = '\uf71b'

    const val IframeOff: Char = '\uf71c'

    const val GoToLine: Char = '\uf71d'

    const val DragPan: Char = '\uf71e'

    const val DragClick: Char = '\uf71f'

    const val DeployedCode: Char = '\uf720'

    const val ClockLoader90: Char = '\uf721'

    const val ClockLoader80: Char = '\uf722'

    const val ClockLoader60: Char = '\uf723'

    const val ClockLoader40: Char = '\uf724'

    const val ClockLoader20: Char = '\uf725'

    const val ClockLoader10: Char = '\uf726'

    const val Capture: Char = '\uf727'

    const val CaptivePortal: Char = '\uf728'

    const val BottomPanelOpen: Char = '\uf729'

    const val BottomPanelClose: Char = '\uf72a'

    const val BackToTab: Char = '\uf72b'

    const val ArrowsOutward: Char = '\uf72c'

    const val ArrowTopRight: Char = '\uf72d'

    const val ArrowTopLeft: Char = '\uf72e'

    const val AppBadging: Char = '\uf72f'

    const val Width: Char = '\uf730'

    const val Ungroup: Char = '\uf731'

    const val TopPanelOpen: Char = '\uf732'

    const val TopPanelClose: Char = '\uf733'

    const val ThumbnailBar: Char = '\uf734'

    const val TextSelectStart: Char = '\uf735'

    const val TextSelectMoveUp: Char = '\uf736'

    const val TextSelectMoveForwardWord: Char = '\uf737'

    const val TextSelectMoveForwardCharacter: Char = '\uf738'

    const val TextSelectMoveDown: Char = '\uf739'

    const val TextSelectMoveBackWord: Char = '\uf73a'

    const val TextSelectMoveBackCharacter: Char = '\uf73b'

    const val TextSelectJumpToEnd: Char = '\uf73c'

    const val TextSelectJumpToBeginning: Char = '\uf73d'

    const val TextSelectEnd: Char = '\uf73e'

    const val TableRowsNarrow: Char = '\uf73f'

    const val TabRecent: Char = '\uf740'

    const val TabNewRight: Char = '\uf741'

    const val TabMove: Char = '\uf742'

    const val TabGroup: Char = '\uf743'

    const val TabDuplicate: Char = '\uf744'

    const val TabClose: Char = '\uf745'

    const val TabCloseRight: Char = '\uf746'

    const val StylusLaserPointer: Char = '\uf747'

    const val StrokePartial: Char = '\uf748'

    const val StrokeFull: Char = '\uf749'

    const val SpecialCharacter: Char = '\uf74a'

    const val SmbShare: Char = '\uf74b'

    const val Signature: Char = '\uf74c'

    const val Select: Char = '\uf74d'

    const val Scan: Char = '\uf74e'

    const val ScanDelete: Char = '\uf74f'

    const val RegularExpression: Char = '\uf750'

    const val PenSize5: Char = '\uf751'

    const val PenSize4: Char = '\uf752'

    const val PenSize3: Char = '\uf753'

    const val PenSize2: Char = '\uf754'

    const val PenSize1: Char = '\uf755'

    const val ListAltAdd: Char = '\uf756'

    const val LineCurve: Char = '\uf757'

    const val LetterSwitch: Char = '\uf758'

    const val LanguageUs: Char = '\uf759'

    const val LanguageUsDvorak: Char = '\uf75a'

    const val LanguageUsColemak: Char = '\uf75b'

    const val LanguagePinyin: Char = '\uf75c'

    const val LanguageKoreanLatin: Char = '\uf75d'

    const val LanguageInternational: Char = '\uf75e'

    const val LanguageGbEnglish: Char = '\uf75f'

    const val LanguageFrench: Char = '\uf760'

    const val LanguageChineseWubi: Char = '\uf761'

    const val LanguageChineseQuick: Char = '\uf762'

    const val LanguageChinesePinyin: Char = '\uf763'

    const val LanguageChineseDayi: Char = '\uf764'

    const val LanguageChineseCangjie: Char = '\uf765'

    const val LanguageChineseArray: Char = '\uf766'

    const val HighlighterSize5: Char = '\uf767'

    const val HighlighterSize4: Char = '\uf768'

    const val HighlighterSize3: Char = '\uf769'

    const val HighlighterSize2: Char = '\uf76a'

    const val HighlighterSize1: Char = '\uf76b'

    const val HeapSnapshotThumbnail: Char = '\uf76c'

    const val HeapSnapshotMultiple: Char = '\uf76d'

    const val HeapSnapshotLarge: Char = '\uf76e'

    const val GridGuides: Char = '\uf76f'

    const val FrameSource: Char = '\uf770'

    const val FrameReload: Char = '\uf771'

    const val FrameInspect: Char = '\uf772'

    const val FormatLetterSpacing: Char = '\uf773'

    const val FolderSupervised: Char = '\uf774'

    const val FolderManaged: Char = '\uf775'

    const val FlexWrap: Char = '\uf776'

    const val FlexNoWrap: Char = '\uf777'

    const val FlexDirection: Char = '\uf778'

    const val FitWidth: Char = '\uf779'

    const val FitPage: Char = '\uf77a'

    const val Equal: Char = '\uf77b'

    const val Counter9: Char = '\uf77c'

    const val Counter8: Char = '\uf77d'

    const val Counter7: Char = '\uf77e'

    const val Counter6: Char = '\uf77f'

    const val Counter5: Char = '\uf780'

    const val Counter4: Char = '\uf781'

    const val Counter3: Char = '\uf782'

    const val Counter2: Char = '\uf783'

    const val Counter1: Char = '\uf784'

    const val Counter0: Char = '\uf785'

    const val AlignStretch: Char = '\uf786'

    const val AlignStart: Char = '\uf787'

    const val AlignSpaceEven: Char = '\uf788'

    const val AlignSpaceBetween: Char = '\uf789'

    const val AlignSpaceAround: Char = '\uf78a'

    const val AlignSelfStretch: Char = '\uf78b'

    const val AlignJustifyStretch: Char = '\uf78c'

    const val AlignJustifySpaceEven: Char = '\uf78d'

    const val AlignJustifySpaceBetween: Char = '\uf78e'

    const val AlignJustifySpaceAround: Char = '\uf78f'

    const val AlignJustifyFlexStart: Char = '\uf790'

    const val AlignJustifyFlexEnd: Char = '\uf791'

    const val AlignJustifyCenter: Char = '\uf792'

    const val AlignItemsStretch: Char = '\uf793'

    const val AlignFlexStart: Char = '\uf794'

    const val AlignFlexEnd: Char = '\uf795'

    const val AlignFlexCenter: Char = '\uf796'

    const val AlignEnd: Char = '\uf797'

    const val GlobeUk: Char = '\uf798'

    const val GlobeAsia: Char = '\uf799'

    const val CookieOff: Char = '\uf79a'

    const val LowDensity: Char = '\uf79b'

    const val HighDensity: Char = '\uf79c'

    const val BackgroundGridSmall: Char = '\uf79d'

    const val BackgroundDotLarge: Char = '\uf79e'

    const val StreamApps: Char = '\uf79f'

    const val PrintError: Char = '\uf7a0'

    const val PrintConnect: Char = '\uf7a1'

    const val PrintAdd: Char = '\uf7a2'

    const val MemoryAlt: Char = '\uf7a3'

    const val HardDrive2: Char = '\uf7a4'

    const val DevicesOff: Char = '\uf7a5'

    const val CameraVideo: Char = '\uf7a6'

    const val WifiProxy: Char = '\uf7a7'

    const val WifiAdd: Char = '\uf7a8'

    const val SignalCellularAdd: Char = '\uf7a9'

    const val PhonelinkRingOff: Char = '\uf7aa'

    const val NetworkManage: Char = '\uf7ab'

    const val ChatError: Char = '\uf7ac'

    const val WarningOff: Char = '\uf7ad'

    const val ShiftLock: Char = '\uf7ae'

    const val PreviewOff: Char = '\uf7af'

    const val DomainVerificationOff: Char = '\uf7b0'

    const val BookmarkManager: Char = '\uf7b1'

    const val AdOff: Char = '\uf7b2'

    const val AccountCircleOff: Char = '\uf7b3'

    const val ConversionPathOff: Char = '\uf7b4'

    const val SelectToSpeak: Char = '\uf7cf'

    const val Resume: Char = '\uf7d0'

    const val FramePersonOff: Char = '\uf7d1'

    const val ScreenshotRegion: Char = '\uf7d2'

    const val ScreenshotKeyboard: Char = '\uf7d3'

    const val OverviewKey: Char = '\uf7d4'

    const val MagnifyFullscreen: Char = '\uf7d5'

    const val MagnifyDocked: Char = '\uf7d6'

    const val MagicTether: Char = '\uf7d7'

    const val LtePlusMobiledataBadge: Char = '\uf7d8'

    const val LteMobiledataBadge: Char = '\uf7d9'

    const val KeyboardPreviousLanguage: Char = '\uf7da'

    const val KeyboardOnscreen: Char = '\uf7db'

    const val KeyboardFull: Char = '\uf7dc'

    const val KeyboardExternalInput: Char = '\uf7dd'

    const val KeyboardCapslockBadge: Char = '\uf7de'

    const val HPlusMobiledataBadge: Char = '\uf7df'

    const val HMobiledataBadge: Char = '\uf7e0'

    const val GMobiledataBadge: Char = '\uf7e1'

    const val EvMobiledataBadge: Char = '\uf7e2'

    const val EMobiledataBadge: Char = '\uf7e3'

    const val DockToRight: Char = '\uf7e4'

    const val DockToLeft: Char = '\uf7e5'

    const val DockToBottom: Char = '\uf7e6'

    const val DisplayExternalInput: Char = '\uf7e7'

    const val BrightnessEmpty: Char = '\uf7e8'

    const val BatteryPlus: Char = '\uf7e9'

    const val BatteryError: Char = '\uf7ea'

    const val BatteryChange: Char = '\uf7eb'

    const val BacklightLow: Char = '\uf7ec'

    const val BacklightHigh: Char = '\uf7ed'

    const val _5gMobiledataBadge: Char = '\uf7ee'

    const val _4gMobiledataBadge: Char = '\uf7ef'

    const val _3gMobiledataBadge: Char = '\uf7f0'

    const val _1xMobiledataBadge: Char = '\uf7f1'

    const val DataCheck: Char = '\uf7f2'

    const val QuestionExchange: Char = '\uf7f3'

    const val MagicExchange: Char = '\uf7f4'

    const val DataInfoAlert: Char = '\uf7f5'

    const val DataAlert: Char = '\uf7f6'

    const val DrawCollage: Char = '\uf7f7'

    const val DrawAbstract: Char = '\uf7f8'

    const val PartnerExchange: Char = '\uf7f9'

    const val Deskphone: Char = '\uf7fa'

    const val Podium: Char = '\uf7fb'

    const val PlayShapes: Char = '\uf7fc'

    const val PersonPlay: Char = '\uf7fd'

    const val PersonCelebrate: Char = '\uf7fe'

    const val InteractiveSpace: Char = '\uf7ff'

    const val SearchCheck: Char = '\uf800'

    const val QuickReferenceAll: Char = '\uf801'

    const val Amend: Char = '\uf802'

    const val Ambulance: Char = '\uf803'

    const val UnknownDocument: Char = '\uf804'

    const val Stethoscope: Char = '\uf805'

    const val StethoscopeCheck: Char = '\uf806'

    const val StethoscopeArrow: Char = '\uf807'

    const val RecentPatient: Char = '\uf808'

    const val PillOff: Char = '\uf809'

    const val MedicalMask: Char = '\uf80a'

    const val LabResearch: Char = '\uf80b'

    const val FluidMed: Char = '\uf80c'

    const val FluidBalance: Char = '\uf80d'

    const val HardDrive: Char = '\uf80e'

    const val Ecg: Char = '\uf80f'

    const val HelpClinic: Char = '\uf810'

    const val OrderPlay: Char = '\uf811'

    const val OrderApprove: Char = '\uf812'

    const val AvgTime: Char = '\uf813'

    const val LineStartSquare: Char = '\uf814'

    const val LineStartDiamond: Char = '\uf815'

    const val LineStartCircle: Char = '\uf816'

    const val LineStartArrowNotch: Char = '\uf817'

    const val LineStartArrow: Char = '\uf818'

    const val LineEndSquare: Char = '\uf819'

    const val LineEndDiamond: Char = '\uf81a'

    const val LineEndCircle: Char = '\uf81b'

    const val LineEndArrowNotch: Char = '\uf81c'

    const val LineEndArrow: Char = '\uf81d'

    const val Sprint: Char = '\uf81f'

    const val SyncSavedLocally: Char = '\uf820'

    const val ChipExtraction: Char = '\uf821'

    const val SlideLibrary: Char = '\uf822'

    const val SheetsRtl: Char = '\uf823'

    const val ResetImage: Char = '\uf824'

    const val LineStart: Char = '\uf825'

    const val LineEnd: Char = '\uf826'

    const val InsertText: Char = '\uf827'

    const val FormatTextWrap: Char = '\uf828'

    const val FormatTextOverflow: Char = '\uf829'

    const val FormatTextClip: Char = '\uf82a'

    const val FormatInkHighlighter: Char = '\uf82b'

    const val DecimalIncrease: Char = '\uf82c'

    const val DecimalDecrease: Char = '\uf82d'

    const val CellMerge: Char = '\uf82e'

    const val ArrowSelectorTool: Char = '\uf82f'

    const val ExpandContent: Char = '\uf830'

    const val SettingsPanorama: Char = '\uf831'

    const val SettingsNightSight: Char = '\uf832'

    const val SettingsMotionMode: Char = '\uf833'

    const val SettingsPhotoCamera: Char = '\uf834'

    const val SettingsAccountBox: Char = '\uf835'

    const val ArrowInsert: Char = '\uf837'

    const val PrayerTimes: Char = '\uf838'

    const val VideoCameraFrontOff: Char = '\uf83b'

    const val MagnificationSmall: Char = '\uf83c'

    const val MagnificationLarge: Char = '\uf83d'

    const val AutoDetectVoice: Char = '\uf83e'

    const val MediaLink: Char = '\uf83f'

    const val MovieEdit: Char = '\uf840'

    const val AttachFileAdd: Char = '\uf841'

    const val MotionMode: Char = '\uf842'

    const val EmptyDashboard: Char = '\uf844'

    const val Rebase: Char = '\uf845'

    const val RebaseEdit: Char = '\uf846'

    const val ViewColumn2: Char = '\uf847'

    const val AssignmentAdd: Char = '\uf848'

    const val FormatListBulletedAdd: Char = '\uf849'

    const val ApprovalDelegation: Char = '\uf84a'

    const val Autopay: Char = '\uf84b'

    const val BusinessChip: Char = '\uf84c'

    const val CodeBlocks: Char = '\uf84d'

    const val FinanceChip: Char = '\uf84e'

    const val LocationChip: Char = '\uf850'

    const val Variables: Char = '\uf851'

    const val VotingChip: Char = '\uf852'

    const val CinematicBlur: Char = '\uf853'

    const val Cycle: Char = '\uf854'

    const val ProcessChart: Char = '\uf855'

    const val Breastfeeding: Char = '\uf856'

    const val Diversity4: Char = '\uf857'

    const val ContactlessOff: Char = '\uf858'

    const val InboxCustomize: Char = '\uf859'

    const val YoutubeActivity: Char = '\uf85a'

    const val CakeAdd: Char = '\uf85b'

    const val BarcodeReader: Char = '\uf85c'

    const val FormatH1: Char = '\uf85d'

    const val FormatH2: Char = '\uf85e'

    const val FormatH3: Char = '\uf85f'

    const val FormatH4: Char = '\uf860'

    const val FormatH5: Char = '\uf861'

    const val FormatH6: Char = '\uf862'

    const val FormatImageLeft: Char = '\uf863'

    const val FormatImageRight: Char = '\uf864'

    const val FormatParagraph: Char = '\uf865'

    const val Function: Char = '\uf866'

    const val ConveyorBelt: Char = '\uf867'

    const val Forklift: Char = '\uf868'

    const val FrontLoader: Char = '\uf869'

    const val Pallet: Char = '\uf86a'

    const val Trolley: Char = '\uf86b'

    const val HomeStorage: Char = '\uf86c'

    const val SelfCare: Char = '\uf86d'

    const val Shelves: Char = '\uf86e'

    const val GalleryThumbnail: Char = '\uf86f'

    const val WaterDo: Char = '\uf870'

    const val Barefoot: Char = '\uf871'

    const val SpecificGravity: Char = '\uf872'

    const val Altitude: Char = '\uf873'

    const val WaterLux: Char = '\uf874'

    const val WaterEc: Char = '\uf875'

    const val Salinity: Char = '\uf876'

    const val TotalDissolvedSolids: Char = '\uf877'

    const val WaterOrp: Char = '\uf878'

    const val DewPoint: Char = '\uf879'

    const val WaterPh: Char = '\uf87a'

    const val WaterVoc: Char = '\uf87b'

    const val Infrared: Char = '\uf87c'

    const val Footprint: Char = '\uf87d'

    const val HumidityPercentage: Char = '\uf87e'

    const val Passkey: Char = '\uf87f'

    const val DirectionsAlt: Char = '\uf880'

    const val DirectionsAltOff: Char = '\uf881'

    const val Robot: Char = '\uf882'

    const val HeartMinus: Char = '\uf883'

    const val HeartPlus: Char = '\uf884'

    const val FormatUnderlinedSquiggle: Char = '\uf885'

    const val FileUploadOff: Char = '\uf886'

    const val ToysFan: Char = '\uf887'

    const val Agender: Char = '\uf888'

    const val Swords: Char = '\uf889'

    const val CheckIndeterminateSmall: Char = '\uf88a'

    const val CheckSmall: Char = '\uf88b'

    const val EditDocument: Char = '\uf88c'

    const val EditSquare: Char = '\uf88d'

    const val ApkDocument: Char = '\uf88e'

    const val ApkInstall: Char = '\uf88f'

    const val Femur: Char = '\uf891'

    const val FemurAlt: Char = '\uf892'

    const val FootBones: Char = '\uf893'

    const val HandBones: Char = '\uf894'

    const val Humerus: Char = '\uf895'

    const val HumerusAlt: Char = '\uf896'

    const val Orthopedics: Char = '\uf897'

    const val RibCage: Char = '\uf898'

    const val Skeleton: Char = '\uf899'

    const val Skull: Char = '\uf89a'

    const val Tibia: Char = '\uf89b'

    const val TibiaAlt: Char = '\uf89c'

    const val UlnaRadius: Char = '\uf89d'

    const val UlnaRadiusAlt: Char = '\uf89e'

    const val AodTablet: Char = '\uf89f'

    const val VideoChat: Char = '\uf8a0'

    const val Camping: Char = '\uf8a2'

    const val Glyphs: Char = '\uf8a3'

    const val ShareReviews: Char = '\uf8a4'

    const val BrowseActivity: Char = '\uf8a5'

    const val FramePerson: Char = '\uf8a6'

    const val SpeechToText: Char = '\uf8a7'

    const val NoiseControlOn: Char = '\uf8a8'

    const val GardenCart: Char = '\uf8a9'

    const val PottedPlant: Char = '\uf8aa'

    const val ArrowsMoreDown: Char = '\uf8ab'

    const val ArrowsMoreUp: Char = '\uf8ac'

    const val AutoActivityZone: Char = '\uf8ad'

    const val BatteryHoriz000: Char = '\uf8ae'

    const val BatteryHoriz050: Char = '\uf8af'

    const val BatteryHoriz075: Char = '\uf8b0'

    const val BatteryVert005: Char = '\uf8b1'

    const val BatteryVert020: Char = '\uf8b2'

    const val BatteryVert050: Char = '\uf8b3'

    const val CleaningBucket: Char = '\uf8b4'

    const val ClimateMiniSplit: Char = '\uf8b5'

    const val NestCamFloodlight: Char = '\uf8b7'

    const val NestCamMagnetMount: Char = '\uf8b8'

    const val NestCamStand: Char = '\uf8b9'

    const val NestCamWallMount: Char = '\uf8ba'

    const val NestClockFarsightAnalog: Char = '\uf8bb'

    const val NestClockFarsightDigital: Char = '\uf8bc'

    const val NestDoorbellVisitor: Char = '\uf8bd'

    const val NestEcoLeaf: Char = '\uf8be'

    const val NestFarsightWeather: Char = '\uf8bf'

    const val NestFoundSavings: Char = '\uf8c0'

    const val NestMultiRoom: Char = '\uf8c2'

    const val NestSunblock: Char = '\uf8c3'

    const val NestTrueRadiant: Char = '\uf8c4'

    const val NestWakeOnApproach: Char = '\uf8c5'

    const val NestWakeOnPress: Char = '\uf8c6'

    const val TamperDetectionOn: Char = '\uf8c8'

    const val TempPreferencesCustom: Char = '\uf8c9'

    const val TempPreferencesEco: Char = '\uf8ca'

    const val ToolsFlatHead: Char = '\uf8cb'

    const val ToolsPhillips: Char = '\uf8cc'

    const val ArrowOutward: Char = '\uf8ce'

    const val UnfoldLessDouble: Char = '\uf8cf'

    const val UnfoldMoreDouble: Char = '\uf8d0'

    const val ContactEmergency: Char = '\uf8d1'

    const val MacroOff: Char = '\uf8d2'

    const val ShapeLine: Char = '\uf8d3'

    const val AssistWalker: Char = '\uf8d5'

    const val Blind: Char = '\uf8d6'

    const val Diversity1: Char = '\uf8d7'

    const val Diversity2: Char = '\uf8d8'

    const val Diversity3: Char = '\uf8d9'

    const val Face2: Char = '\uf8da'

    const val Face3: Char = '\uf8db'

    const val Face4: Char = '\uf8dc'

    const val Face5: Char = '\uf8dd'

    const val Face6: Char = '\uf8de'

    const val Groups2: Char = '\uf8df'

    const val Groups3: Char = '\uf8e0'

    const val Man2: Char = '\uf8e1'

    const val Man3: Char = '\uf8e2'

    const val Man4: Char = '\uf8e3'

    const val Person2: Char = '\uf8e4'

    const val Person3: Char = '\uf8e5'

    const val Person4: Char = '\uf8e6'

    const val Woman2: Char = '\uf8e7'

    const val Repartition: Char = '\uf8e8'

    const val PsychologyAlt: Char = '\uf8ea'

    const val AddHome: Char = '\uf8eb'

    const val Transcribe: Char = '\uf8ec'

    const val AddHomeWork: Char = '\uf8ed'

    const val Dataset: Char = '\uf8ee'

    const val DatasetLinked: Char = '\uf8ef'

    const val TypeSpecimen: Char = '\uf8f0'

    const val FireTruck: Char = '\uf8f2'

    const val LockPerson: Char = '\uf8f3'

    const val Desk: Char = '\uf8f4'

    const val WidthFull: Char = '\uf8f5'

    const val WidthNormal: Char = '\uf8f6'

    const val WidthWide: Char = '\uf8f7'

    const val BroadcastOnHome: Char = '\uf8f8'

    const val BroadcastOnPersonal: Char = '\uf8f9'

    const val _18UpRating: Char = '\uf8fd'

    const val NoAdultContent: Char = '\uf8fe'

    const val Wallet: Char = '\uf8ff'
}
