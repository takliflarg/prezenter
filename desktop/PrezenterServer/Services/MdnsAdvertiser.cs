using System;
using Makaretu.Dns;

namespace PrezenterServer.Services;

/// <summary>
/// Kompyuterni lokal tarmoqda "_prezenter._tcp.local." xizmati sifatida
/// e'lon qiladi, shunda Android ilova QR kodsiz ham (bir marta
/// tasdiqlangandan keyin) uni ro'yxatda avtomatik ko'rsata oladi.
/// </summary>
public sealed class MdnsAdvertiser : IDisposable
{
    private const string ServiceType = "_prezenter._tcp";

    private readonly ServiceDiscovery _discovery = new();
    private ServiceProfile? _profile;

    public void Start(int port)
    {
        var instanceName = $"{Environment.MachineName}-{ServiceType}";
        _profile = new ServiceProfile(instanceName, ServiceType, (ushort)port);
        _profile.AddProperty("name", Environment.MachineName);
        _profile.AddProperty("version", "1");

        _discovery.Advertise(_profile);
    }

    public void Stop()
    {
        if (_profile is not null)
        {
            _discovery.Unadvertise(_profile);
        }
    }

    public void Dispose()
    {
        Stop();
        _discovery.Dispose();
    }
}
