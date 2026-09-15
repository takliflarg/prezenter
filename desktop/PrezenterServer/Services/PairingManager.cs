using System.Security.Cryptography;
using System.Text;
using System.Text.Json;

namespace PrezenterServer.Services;

public sealed record ApprovedDevice(string Token, string DeviceName, DateTime ApprovedAtUtc);

/// <summary>
/// Pairing oqimi:
///  1. Kompyuter foydalanuvchisi "Yangi qurilma qo'shish" tugmasini bosadi ->
///     bir martalik, 5 daqiqa amal qiladigan token yaratiladi va QR kod
///     sifatida ko'rsatiladi (MainWindow buni ChallengeToken orqali oladi).
///  2. Telefon QR kodni skanerlab, shu tokenni "pair" xabarida yuboradi.
///  3. Token to'g'ri bo'lsa, u doimiy "tasdiqlangan qurilmalar" ro'yxatiga
///     ko'chadi (diskka saqlanadi) - shu bois keyingi safar telefon xuddi
///     shu tokenni yuborsa, QR qayta ko'rsatilmasdan avtomatik ulanadi.
///     Android tomonida bu token NSD orqali topilgan kompyuterga ulanishda
///     ham ishlatiladi (docs/PROTOCOL.md).
/// </summary>
public sealed class PairingManager
{
    private static readonly string StorePath = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
        "PrezenterServer", "approved-devices.json");

    private readonly object _lock = new();
    private readonly Dictionary<string, ApprovedDevice> _approvedByToken = new();
    private readonly Dictionary<string, string> _pairedConnections = new(); // connectionId -> token

    private string? _pendingChallengeToken;
    private DateTime _pendingChallengeExpiresUtc;

    public event Action<ApprovedDevice>? DeviceApproved;

    public PairingManager()
    {
        Load();
    }

    /// <summary>Yangi bir martalik QR pairing tokenini yaratadi (5 daqiqa amal qiladi).</summary>
    public string GenerateChallengeToken()
    {
        lock (_lock)
        {
            _pendingChallengeToken = Convert.ToBase64String(RandomNumberGenerator.GetBytes(24));
            _pendingChallengeExpiresUtc = DateTime.UtcNow.AddMinutes(5);
            return _pendingChallengeToken;
        }
    }

    public bool IsPaired(string connectionId)
    {
        lock (_lock)
        {
            return _pairedConnections.ContainsKey(connectionId);
        }
    }

    public bool TryPair(string connectionId, string? token, string deviceName)
    {
        if (string.IsNullOrWhiteSpace(token))
        {
            return false;
        }

        lock (_lock)
        {
            // Holat 1: allaqachon tasdiqlangan qurilma qayta ulanmoqda.
            if (_approvedByToken.ContainsKey(token))
            {
                _pairedConnections[connectionId] = token;
                return true;
            }

            // Holat 2: yangi qurilma, QR orqali olingan bir martalik tokenni tasdiqlamoqda.
            if (_pendingChallengeToken == token && DateTime.UtcNow <= _pendingChallengeExpiresUtc)
            {
                var approved = new ApprovedDevice(token, deviceName, DateTime.UtcNow);
                _approvedByToken[token] = approved;
                _pairedConnections[connectionId] = token;
                _pendingChallengeToken = null; // bir martalik - qayta ishlatilmaydi
                Save();
                DeviceApproved?.Invoke(approved);
                return true;
            }

            return false;
        }
    }

    public void Disconnect(string connectionId)
    {
        lock (_lock)
        {
            _pairedConnections.Remove(connectionId);
        }
    }

    public IReadOnlyList<ApprovedDevice> GetApprovedDevices()
    {
        lock (_lock)
        {
            return _approvedByToken.Values.ToList();
        }
    }

    public void RevokeDevice(string token)
    {
        lock (_lock)
        {
            _approvedByToken.Remove(token);
            Save();
        }
    }

    private void Load()
    {
        try
        {
            if (!File.Exists(StorePath))
            {
                return;
            }

            var json = File.ReadAllText(StorePath);
            var devices = JsonSerializer.Deserialize<List<ApprovedDevice>>(json) ?? new();
            lock (_lock)
            {
                foreach (var device in devices)
                {
                    _approvedByToken[device.Token] = device;
                }
            }
        }
        catch (Exception)
        {
            // Buzilgan/saqlanmagan fayl - bo'sh ro'yxat bilan boshlaymiz.
        }
    }

    private void Save()
    {
        try
        {
            var dir = Path.GetDirectoryName(StorePath)!;
            Directory.CreateDirectory(dir);
            var json = JsonSerializer.Serialize(_approvedByToken.Values.ToList());
            File.WriteAllText(StorePath, json, Encoding.UTF8);
        }
        catch (Exception)
        {
            // Diskka yoza olmasak ham, joriy sessiya davomida pairing xotirada ishlayveradi.
        }
    }
}
