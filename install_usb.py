"""Install APK to all connected Android devices via ppadb"""
import sys
import os
import struct
from ppadb.client import Client

APK_DIR = r"c:\Users\mexs\OneDrive\Project\AI\project\Concept\multica APP\app\build\outputs\apk\debug"

ABI_APK = {
    "arm64-v8a": os.path.join(APK_DIR, "app-arm64-v8a-debug.apk"),
    "armeabi-v7a": os.path.join(APK_DIR, "app-armeabi-v7a-debug.apk"),
    "x86": os.path.join(APK_DIR, "app-x86-debug.apk"),
    "x86_64": os.path.join(APK_DIR, "app-x86_64-debug.apk"),
}
UNIVERSAL = os.path.join(APK_DIR, "app-universal-debug.apk")

def install_apk(device, apk_path):
    """Install APK using pm install with -r -t flags (replace + test)"""
    apk_name = os.path.basename(apk_path)
    # Push APK to device
    dest = f"/data/local/tmp/{apk_name}"
    print(f"  Pushing {apk_name}...")
    device.push(apk_path, dest)

    # Install using pm install -r -t (replace + allow test APK)
    print(f"  Installing...")
    result = device.shell(f"pm install -r -t {dest}")
    print(f"  pm install result: {result.strip()}")

    # Cleanup
    device.shell(f"rm {dest}")
    return "Success" in result

def main():
    print("Connecting to ADB server...")
    c = Client(host='127.0.0.1', port=5037)
    devs = c.devices()

    if not devs:
        print("No devices found.")
        return 1

    print(f"Found {len(devs)} device(s)\n")

    for d in devs:
        abi = d.shell("getprop ro.product.cpu.abi").strip()
        model = d.shell("getprop ro.product.model").strip()
        print(f"Device: {d.serial}")
        print(f"  Model: {model}")
        print(f"  ABI: {abi}")

        apk = ABI_APK.get(abi, UNIVERSAL)
        if not os.path.exists(apk):
            apk = UNIVERSAL
        if not os.path.exists(apk):
            print("  APK not found!")
            continue

        print(f"  APK: {os.path.basename(apk)}")
        try:
            success = install_apk(d, apk)
            if success:
                print("  OK - installed successfully")
            else:
                # Try uninstall first then install fresh
                print("  Trying uninstall + fresh install...")
                d.shell("pm uninstall com.multica.app")
                success = install_apk(d, apk)
                print(f"  {'OK' if success else 'FAILED'}")
        except Exception as e:
            print(f"  Error: {e}")
        print()

    return 0

if __name__ == "__main__":
    sys.exit(main())
