# QR File Share

**Secure file sharing made simple.** Upload files once, share via QR code instantly. With password protection, automatic expiry, and one-time download tokens—your files stay secure.

---

## Overview

QR File Share is a web application designed for secure, temporary file sharing. Users can upload files, protect them with optional passwords, set automatic expiry times, and share them instantly via dynamically generated QR codes. Perfect for confidential documents, sensitive data, or any file you want to share with controlled access.

### Key Highlights

- **QR Code Sharing**: Generate shareable QR codes for instant access
- **Password Protection**: Optional password-based access control
- **Auto-Expiry**: Files automatically expire and are deleted after a set time
- **One-Time Tokens**: 60-second download tokens prevent unauthorized access
- **Secure Storage**: Integration with Supabase Storage for reliable file management
- **Automatic Cleanup**: Scheduled deletion of expired files and orphaned data

---

## Features

### 🔐 Security
- **BCrypt Password Hashing**: Passwords are never stored in plain text
- **Signed URLs**: Time-limited download URLs (Supabase integration)
- **Token-Based Downloads**: One-time use 60-second tokens for file retrieval
- **Expiry Management**: Automatic deletion of expired files from storage and database
- **CORS Configuration**: Secure cross-origin requests with origin pattern validation

### 📤 File Management
- **Multi-Format Support**: Upload any file type up to configured limits (50MB)
- **Unique Identifiers**: UUID-based file tracking
- **Download Tracking**: Monitor download count for each file
- **Batch Cleanup**: Scheduled removal of expired files every 60 seconds

### 🎯 User Experience
- **Clean Web Interface**: Responsive HTML/CSS/JavaScript frontend
- **Simple Upload Flow**: Straightforward file upload with optional security options
- **Instant QR Generation**: Share files immediately after upload
- **Password Verification API**: Secure endpoint for access validation

### 🛠️ System Reliability
- **Database Persistence**: PostgreSQL for metadata storage
- **Scheduled Tasks**: Spring scheduling for automated cleanup operations
- **Error Handling**: Comprehensive logging and exception management
- **RESTful API**: Well-structured endpoints for core operations

---

## Architecture

### Technology Stack

| Layer | Technology |
|-------|-----------|
| **Backend Framework** | Spring Boot 4.0.3 |
| **Language** | Java 17 |
| **Web Framework** | Spring MVC + Thymeleaf |
| **Security** | Spring Security 6 |
| **Database** | PostgreSQL (JPA/Hibernate) |
| **File Storage** | Supabase Storage (S3-compatible) |
| **Frontend** | HTML5, CSS3, JavaScript |
| **Build Tool** | Maven |
| **Containerization** | Docker |

### Project Structure

```
src/main/java/com/qrfileshare/
├── config/
│   ├── AppConfig.java          # BCrypt encoder, RestTemplate config
│   └── SecurityConfig.java      # CORS, authentication, authorization
├── controller/
│   ├── HomeController.java      # Landing page
│   └── PasswordVerifyController.java  # Password validation & token issuance
├── service/
│   ├── FileShareService.java    # Core business logic
│   ├── FileStorageService.java  # Supabase integration
│   ├── DownloadTokenStore.java  # One-time token management
│   └── CleanupScheduler.java    # Automated expiry cleanup
├── model/
│   └── FileShare.java           # JPA entity for file metadata
├── repository/
│   └── FileShareRepository.java # Database access layer
└── QrFileShareApplication.java  # Main entry point
```

### Core Components

#### **FileShare Entity** (`model/FileShare.java`)
Represents uploaded file metadata:
- **UUID ID**: Unique identifier for each shared file
- **File Name**: Original filename
- **Storage Path**: Location in Supabase Storage
- **Password Hash**: BCrypt-encrypted password (optional)
- **Expiry Time**: Automatic deletion timestamp
- **Download Count**: Track number of downloads
- **Created At**: Upload timestamp
- **Helper Methods**: `isExpired()`, `isPasswordProtected()`

#### **File Storage Service** (`service/FileStorageService.java`)
Handles Supabase interaction:
- Uploads files to Supabase Storage bucket
- Generates short-lived signed URLs (60-second expiry)
- Deletes files from cloud storage
- Manages authentication via service key

#### **Download Token Store** (`service/DownloadTokenStore.java`)
One-time token system:
- Issues 60-second valid tokens for verified downloads
- Validates and consumes tokens (single-use)
- Automatically cleans up expired tokens every minute
- Uses in-memory `ConcurrentHashMap` for fast access

#### **Cleanup Scheduler** (`service/CleanupScheduler.java`)
Automated maintenance:
- Runs every 60 seconds
- Identifies expired files by `expiryTime`
- Deletes from Supabase Storage
- Removes metadata from PostgreSQL database
- Comprehensive error logging

#### **Security Configuration** (`config/SecurityConfig.java`)
Spring Security setup:
- CORS enabled for Chrome Extension and local development
- All endpoints publicly accessible (auth handled manually)
- CSRF disabled for cross-origin requests
- No form login or HTTP Basic Auth

---

## API Endpoints

### File Upload & Share
```
POST /api/upload
Content-Type: multipart/form-data

Parameters:
  - file: MultipartFile (required)
  - password: String (optional)
  - expiryHours: Integer (optional, default: 24)

Response:
  {
    "fileId": "uuid",
    "qrCode": "data:image/png;base64,...",
    "shareUrl": "https://..."
  }
```

### Password Verification
```
POST /api/verify
Content-Type: application/json

Body:
  {
    "fileId": "uuid",
    "password": "user-password"
  }

Response:
  {
    "streamToken": "one-time-token"
  }

Errors:
  - 400: Missing fileId or password
  - 404: File not found
  - 410: File expired
  - 401: Incorrect password
```

### File Download
```
GET /api/download/:fileId?token=one-time-token

Response: File binary data
Status Codes:
  - 200: Success
  - 401: Invalid or expired token
  - 404: File not found
  - 410: File expired
```

---

## File Lifecycle

```
┌─────────────────────────────────────────────────────┐
│ 1. User Uploads File                                │
│    - File → Supabase Storage                        │
│    - Metadata → PostgreSQL                          │
│    - Password (optional) → BCrypt Hash              │
└──────────────────┬──────────────────────────────────┘
                   ↓
┌─────────────────────────────────────────────────────┐
│ 2. QR Code Generated & Shared                       │
│    - Contains share URL with file ID                │
└──────────────────┬──────────────────────────────────┘
                   ↓
         ┌─────────┴─────────┐
         ↓                   ↓
    [Password         [No Password
     Required]        Required]
         │                   │
         └─────────┬─────────┘
                   ↓
┌─────────────────────────────────────────────────────┐
│ 3. Access Verification                              │
│    - Password validation (if protected)             │
│    - Expiry check                                   │
│    - One-time token issued (60 seconds)             │
└──────────────────┬──────────────────────────────────┘
                   ↓
┌─────────────────────────────────────────────────────┐
│ 4. Secure Download                                  │
│    - Token validated & consumed                     │
│    - Signed URL generated (Supabase)                │
│    - File downloaded                                │
│    - Download count incremented                     │
└──────────────────┬──────────────────────────────────┘
                   ↓
         ┌─────────┴──────────┐
         ↓                    ↓
    [Expiry Check]    [Scheduled Cleanup]
         │                    │
         └────┬───────────────┘
              ↓
┌─────────────────────────────────────────────────────┐
│ 5. Automatic Cleanup (Every 60 seconds)             │
│    - Identify expired files                         │
│    - Delete from Supabase Storage                   │
│    - Remove metadata from PostgreSQL                │
│    - Clean up orphaned tokens                       │
└─────────────────────────────────────────────────────┘
```

---

## Security Considerations

### Password Security
- Passwords are hashed using **BCrypt** with default cost factor
- Never stored or transmitted in plain text
- Verified on each access attempt

### Network Security
- **HTTPS recommended** in production
- **CORS validation** prevents unauthorized cross-origin access
- **CSRF protection** enabled where applicable

### File Access
- **Signed URLs** with 60-second expiry limit unauthorized access
- **One-time tokens** ensure downloads can't be replayed
- **UUID identifiers** prevent enumeration attacks

### Data Cleanup
- Automatic deletion prevents accumulation of stale data
- Failed deletions are logged for manual review
- Token cleanup prevents memory leaks

---

## Performance & Scalability

- **Database Indexing**: File lookup by UUID and expiry time optimized
- **Asynchronous File Operations**: Non-blocking uploads/downloads
- **Connection Pooling**: HTTP client with pooling for Supabase requests
- **Scheduled Cleanup**: Offloads expiry management from request processing
- **Token Store**: In-memory store for fast validation (O(1) lookup)

---

## Support

**Found a bug?** [Create an issue](https://github.com/UpadhyayAnurag/qr-file-share/issues)  
**Want to contribute?** Pull requests are welcome!

---

## Author

**Anurag Upadhyay**  
[GitHub](https://github.com/UpadhyayAnurag)

---

**Made with ❤️ for secure file sharing**
