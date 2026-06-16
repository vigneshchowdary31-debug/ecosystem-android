import os

path = "/Users/vigneshchowdary/Library/Developer/Xcode/DerivedData"
target = b"KEYCHAIN_DUMP_QR_PAYLOAD"

found = False
for root, dirs, files in os.walk(path):
    for f in files:
        filepath = os.path.join(root, f)
        try:
            with open(filepath, 'rb') as fp:
                content = fp.read()
                if target in content:
                    idx = content.index(target)
                    snippet = content[idx:idx+250]
                    print(f"Found in {filepath}:")
                    print(snippet.decode('utf-8', errors='ignore'))
                    found = True
        except Exception as e:
            pass

if not found:
    print("Not found anywhere in DerivedData.")
