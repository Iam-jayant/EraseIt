# 🔥 EraseIt - USB Drive Wiping Guide

**IMPORTANT**: This guide ensures you can safely wipe your USB drive while keeping your SSD/HDD completely protected.

## 🛡️ Safety Guarantees

### ✅ Your SSD/HDD Will Be Protected By:
1. **System Drive Detection** - C:\ drive is automatically blocked
2. **Drive Type Filtering** - Only removable USB drives are allowed
3. **Size Limits** - Drives larger than 2TB are blocked (your SSD/HDD are safe)
4. **Multiple Confirmations** - Several safety checks before wiping
5. **Type-to-Confirm** - Must type 'WIPE' exactly to proceed
6. **Visual Safety Indicators** - Clear safety status for each drive

### ❌ What Cannot Be Wiped:
- C:\\ drive (Windows system drive)
- Any internal SSD or HDD
- Network drives  
- Optical drives (CD/DVD)
- Drives larger than 2TB
- Any drive marked as "SYSTEM DRIVE"

## 🚀 Step-by-Step USB Wiping Process

### Step 1: Prepare Your USB Drive
```
1. Insert the USB drive you want to wipe
2. Backup any important data (this will be PERMANENTLY deleted)
3. Note the USB drive's current contents for verification
4. Ensure you have the correct USB drive
```

### Step 2: Launch EraseIt
```powershell
# Run this command in PowerShell
.\run-usb-wiper.ps1
```

Or manually:
```powershell
mvn javafx:run
```

### Step 3: Device Selection (CRITICAL STEP)
```
1. Click "🔄 Refresh Devices" button
2. Look at the device list - you should see:
   - Your USB drive (removable, smaller size)
   - Your system drives (will be blocked/hidden)
3. Select your USB drive from the dropdown
4. Verify the safety status shows: "✅ SAFE - Removable USB drive"
5. Check device info shows correct size and label
```

### Step 4: Choose Wipe Method
| Method | Time | Security Level | Recommended For |
|--------|------|----------------|-----------------|
| **Single Pass** | Fast | Good | Quick wipe, modern USB drives |
| **DoD 3-Pass** | Medium | Excellent | **RECOMMENDED** - Government standard |
| **Gutmann 35-Pass** | Very Slow | Maximum | Highly sensitive data |

**Recommendation**: Use **DoD 3-Pass** for your USB drive.

### Step 5: Execute Wipe
```
1. Click "Start Secure Wipe" button
2. Read the detailed confirmation dialog carefully
3. Verify all information is correct:
   - Path matches your USB drive
   - Size matches your USB drive  
   - Type shows "USB REMOVABLE"
   - Safety status is "SAFE"
4. Type "WIPE" exactly (case-sensitive) in the text field
5. Press OK to start wiping
```

### Step 6: Monitor Progress
```
- Watch the progress bar
- Monitor the log area for status updates
- DO NOT disconnect USB drive during wiping
- Process may take 10-60 minutes depending on size and method
```

### Step 7: Save Certificate
```
1. After completion, click "Save Certificate"
2. Choose a location to save the proof documents
3. Two files will be created:
   - PDF certificate with QR code
   - JSON certificate for verification
```

## 🔍 Visual Safety Indicators

### Safe Device (USB Drive):
```
✅ SAFE - Removable USB drive
💾 MyUSB | 📊 32.0 GB | 🏷️ USB REMOVABLE | 🇮🇩 ABC123456
```

### Blocked Device (System Drive):
```
❌ SYSTEM DRIVE - Cannot wipe (contains Windows OS)  
💾 Windows (C:\) | 📊 500.0 GB | 🏷️ HARD DISK | 🇮🇩 XYZ789012
```

## 🎯 USB Drive Wipe Methods Explained

### For Your USB Drive, Choose:

**DoD 3-Pass (RECOMMENDED)** 
- ✅ Government-grade security standard
- ✅ Balances security and time
- ✅ Suitable for financial, personal, business data
- ⏱️ Takes ~30-45 minutes for 32GB USB

**Single Pass (Quick Option)**
- ✅ Fast and effective for modern USB drives
- ✅ Good for non-sensitive data
- ⏱️ Takes ~10-15 minutes for 32GB USB

**Gutmann 35-Pass (Maximum Security)**
- ✅ Absolute maximum security (overkill for most users)
- ✅ Use only for highly sensitive data
- ⏱️ Takes ~2-4 hours for 32GB USB

## ⚠️ Pre-Wipe Checklist

Before wiping your USB drive, verify:

- [ ] **Correct USB drive selected** - Double-check the label and size
- [ ] **Important data backed up** - Copy files you want to keep
- [ ] **USB drive is not write-protected** - Remove any write protection
- [ ] **Safety status shows green** - Must show "✅ SAFE - Removable USB drive"
- [ ] **System drive is not selected** - Should not see C:\ or large drives
- [ ] **Sufficient time available** - DoD 3-Pass takes 30-45 minutes
- [ ] **Stable power connection** - Don't let laptop battery die during wipe

## 🚨 Emergency Stop Procedures

### If You Need to Stop:
1. **Close the application window immediately**
2. **Do not remove the USB drive** if wipe is in progress
3. **Wait for any processes to complete** before disconnecting
4. **Check Task Manager** for any remaining processes

### Red Flags - Stop Immediately:
- ❌ C:\ drive appears in selection
- ❌ Drive size shows your SSD size (500GB+)
- ❌ Safety status shows "SYSTEM DRIVE"
- ❌ Multiple drives selected somehow
- ❌ Progress bar starts on system drive

## 📊 Expected Results

### Successful USB Wipe:
```
✅ Wipe operation completed successfully
✅ Method: DoD 3-Pass
✅ Bytes wiped: 32,212,254,720 bytes
✅ Duration: PT45M30.123S
✅ Digital certificate generated
✅ Certificate ID: CERT-1642345678-ABC123
✅ Public Key Hash: Kj8+9fGH3mN...
✅ Certificates saved successfully
```

### After Wiping:
- USB drive will appear as "unallocated space"
- Windows will ask to format the drive
- All previous data is cryptographically destroyed
- Drive can be safely reused or disposed

## 🎯 Data Recovery Prevention

Your wiped USB drive will be protected against:
- ✅ **Software recovery tools** (Recuva, PhotoRec, etc.)
- ✅ **Professional data recovery** services  
- ✅ **Forensic analysis** tools
- ✅ **Advanced techniques** (depending on method chosen)

**DoD 3-Pass ensures** your data meets government destruction standards.

## 📋 Post-Wipe Actions

### After successful wipe:
1. **Verify completion** - Check all logs show success
2. **Save certificates** - Store proof documents safely  
3. **Test USB drive** - Try formatting and using normally
4. **Document wipe** - Keep certificate for compliance records

### Certificate Uses:
- **Legal compliance** - Proof of secure data destruction
- **Audit trails** - Corporate data governance
- **Verification** - QR code allows mobile verification
- **Insurance** - Evidence for data breach prevention

## 🎉 Success! Your Data Is Secure

Once EraseIt completes:
- ✅ Your USB drive data is **cryptographically destroyed**
- ✅ Data **cannot be recovered** by any standard means
- ✅ You have **legal proof** of secure destruction
- ✅ Your **SSD/HDD remain completely safe** and untouched
- ✅ USB drive can be **safely reused or disposed**

**Congratulations! You've successfully secured your data! 🎯**