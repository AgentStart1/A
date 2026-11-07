# Reads the standard UIA range value from the exact Appium element on the target desktop.
# DesktopDriver 2.8 lacks RangeValue read properties; use its remote PowerShell API.
# https://github.com/verisoft-ai/appium-desktop-driver/blob/v2.8.0/csharp/DesktopDriverServer/Server/ConditionBuilder.cs
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName UIAutomationClient
Add-Type -AssemblyName UIAutomationTypes
$window = [IntPtr]::new([Convert]::ToInt64('__WINDOW_HANDLE__', 16))
$root = [System.Windows.Automation.AutomationElement]::FromHandle($window)
$condition = [System.Windows.Automation.PropertyCondition]::new(
    [System.Windows.Automation.AutomationElement]::ControlTypeProperty,
    [System.Windows.Automation.ControlType]::Slider)
$sliders = $root.FindAll([System.Windows.Automation.TreeScope]::Subtree, $condition)
$matches = @($sliders | Where-Object { ($_.GetRuntimeId() -join '.') -eq '__RUNTIME_ID__' })
if ($matches.Count -ne 1) { throw 'The native volume slider is no longer available' }
$pattern = $matches[0].GetCurrentPattern([System.Windows.Automation.RangeValuePattern]::Pattern)
$pattern.Current.Value.ToString([System.Globalization.CultureInfo]::InvariantCulture)
