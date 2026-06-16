import Foundation
import Security
import CryptoKit

func getKeychainValue(account: String) -> String? {
    let query: [String: Any] = [
        kSecClass as String: kSecClassGenericPassword,
        kSecAttrService as String: "com.ecosystem.ContinuityMac",
        kSecAttrAccount as String: account,
        kSecReturnData as String: true,
        kSecMatchLimit as String: kSecMatchLimitOne
    ]
    var dataTypeRef: AnyObject?
    let status = SecItemCopyMatching(query as CFDictionary, &dataTypeRef)
    guard status == errSecSuccess, let data = dataTypeRef as? Data else {
        return nil
    }
    return String(data: data, encoding: .utf8)
}

func getKeychainData(account: String) -> Data? {
    let query: [String: Any] = [
        kSecClass as String: kSecClassGenericPassword,
        kSecAttrService as String: "com.ecosystem.ContinuityMac",
        kSecAttrAccount as String: account,
        kSecReturnData as String: true,
        kSecMatchLimit as String: kSecMatchLimitOne
    ]
    var dataTypeRef: AnyObject?
    let status = SecItemCopyMatching(query as CFDictionary, &dataTypeRef)
    guard status == errSecSuccess, let data = dataTypeRef as? Data else {
        return nil
    }
    return data
}

guard let deviceName = getKeychainValue(account: "com.ecosystem.ContinuityMac.deviceName"),
      let deviceId = getKeychainValue(account: "com.ecosystem.ContinuityMac.deviceId"),
      let advertisingIdentifier = getKeychainValue(account: "com.ecosystem.ContinuityMac.advertisingIdentifier"),
      let rawData = getKeychainData(account: "com.ecosystem.ContinuityMac.identityPrivateKey") else {
    print("Failed to read all keychain values")
    exit(1)
}

do {
    let privateKey = try Curve25519.Signing.PrivateKey(rawRepresentation: rawData)
    let publicKeyBase64 = privateKey.publicKey.rawRepresentation.base64EncodedString()
    print("deviceName: \(deviceName)")
    print("deviceId: \(deviceId)")
    print("advertisingIdentifier: \(advertisingIdentifier)")
    print("publicKeyEd25519: \(publicKeyBase64)")
    print("QR Payload: identity_continuity:\(deviceId),\(deviceName),\(advertisingIdentifier),\(publicKeyBase64)")
} catch {
    print("Error parsing private key: \(error)")
    exit(1)
}
