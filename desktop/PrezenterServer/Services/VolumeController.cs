using System;
using NAudio.CoreAudioApi;

namespace PrezenterServer.Services;

/// <summary>
/// Windows tizim (master) ovoz balandligi va mute holatini
/// WASAPI default audio endpoint orqali boshqaradi.
/// </summary>
public sealed class VolumeController : IDisposable
{
    private const float StepSize = 0.05f; // har bosishda 5%

    private readonly MMDeviceEnumerator _enumerator = new();
    private MMDevice? _device;

    public bool IsMuted => Device?.AudioEndpointVolume.Mute ?? false;

    public int VolumePercent =>
        Device is null ? 0 : (int)Math.Round(Device.AudioEndpointVolume.MasterVolumeLevelScalar * 100);

    private MMDevice? Device
    {
        get
        {
            try
            {
                _device ??= _enumerator.GetDefaultAudioEndpoint(DataFlow.Render, Role.Multimedia);
                return _device;
            }
            catch (Exception)
            {
                // Faol audio qurilma topilmadi (masalan, hech qanday speaker ulanmagan)
                return null;
            }
        }
    }

    public void StepVolume(int direction)
    {
        var device = Device;
        if (device is null)
        {
            return;
        }

        if (device.AudioEndpointVolume.Mute && direction > 0)
        {
            device.AudioEndpointVolume.Mute = false;
        }

        var current = device.AudioEndpointVolume.MasterVolumeLevelScalar;
        var next = Math.Clamp(current + direction * StepSize, 0f, 1f);
        device.AudioEndpointVolume.MasterVolumeLevelScalar = next;
    }

    public void ToggleMute()
    {
        var device = Device;
        if (device is null)
        {
            return;
        }

        device.AudioEndpointVolume.Mute = !device.AudioEndpointVolume.Mute;
    }

    public void Dispose()
    {
        _device?.Dispose();
        _enumerator.Dispose();
    }
}
