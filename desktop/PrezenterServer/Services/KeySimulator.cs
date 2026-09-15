using System.Runtime.InteropServices;

namespace PrezenterServer.Services;

/// <summary>
/// Win32 SendInput orqali klaviatura tugmalarini simulyatsiya qiladi.
/// PowerPoint prezentatsiya rejimi va Google Slides (brauzerda) uchun
/// standart qisqa yo'llarni yuboradi - docs/PROTOCOL.md dagi jadvalga qarang.
/// </summary>
public sealed class KeySimulator
{
    [DllImport("user32.dll", SetLastError = true)]
    private static extern uint SendInput(uint nInputs, INPUT[] pInputs, int cbSize);

    [StructLayout(LayoutKind.Sequential)]
    private struct INPUT
    {
        public uint type;
        public InputUnion u;
    }

    [StructLayout(LayoutKind.Explicit)]
    private struct InputUnion
    {
        [FieldOffset(0)] public KEYBDINPUT ki;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct KEYBDINPUT
    {
        public ushort wVk;
        public ushort wScan;
        public uint dwFlags;
        public uint time;
        public IntPtr dwExtraInfo;
    }

    private const uint InputKeyboard = 1;
    private const uint KeyEventFKeyUp = 0x0002;

    private const ushort VkLeft = 0x25;
    private const ushort VkRight = 0x27;
    private const ushort VkEscape = 0x1B;
    private const ushort VkF5 = 0x74;
    private const ushort VkShift = 0x10;
    private const ushort VkNext = 0x22; // Page Down
    private const ushort VkPrior = 0x21; // Page Up

    public void SendNextSlide() => PressKey(VkNext);
    public void SendPreviousSlide() => PressKey(VkPrior);
    public void SendStopPresentation() => PressKey(VkEscape);
    public void SendStartFromBeginning() => PressKey(VkF5);
    public void SendStartFromCurrentSlide() => PressKeyWithModifier(VkShift, VkF5);

    private static void PressKey(ushort vk)
    {
        var inputs = new[]
        {
            BuildKeyInput(vk, keyUp: false),
            BuildKeyInput(vk, keyUp: true)
        };
        SendInput((uint)inputs.Length, inputs, Marshal.SizeOf<INPUT>());
    }

    private static void PressKeyWithModifier(ushort modifierVk, ushort vk)
    {
        var inputs = new[]
        {
            BuildKeyInput(modifierVk, keyUp: false),
            BuildKeyInput(vk, keyUp: false),
            BuildKeyInput(vk, keyUp: true),
            BuildKeyInput(modifierVk, keyUp: true)
        };
        SendInput((uint)inputs.Length, inputs, Marshal.SizeOf<INPUT>());
    }

    private static INPUT BuildKeyInput(ushort vk, bool keyUp) => new()
    {
        type = InputKeyboard,
        u = new InputUnion
        {
            ki = new KEYBDINPUT
            {
                wVk = vk,
                wScan = 0,
                dwFlags = keyUp ? KeyEventFKeyUp : 0,
                time = 0,
                dwExtraInfo = IntPtr.Zero
            }
        }
    };
}
