# Video Transcoder
Cloud-native video processing platform built with Java 21, Spring Boot,
FFmpeg, PostgreSQL, and AWS S3.

Designed to automate common video post-processing workflows that would
otherwise require manually running FFmpeg commands.
> *Processes uploaded videos through automated media pipelines including
> silence trimming, compression, audio extraction, and proxy generation.
> Processing jobs are tracked in PostgreSQL and generated assets are
> delivered through secure AWS S3 presigned URLs.*
---
## Features

- Automatic silence trimming
- Video compression and optimization
- Audio extraction and transcoding
- Proxy video generation
- Secure asset storage with AWS S3
- PostgreSQL-backed metadata tracking
- REST API for media processing workflows
---
## Architecture

```text
Client
   │
   ▼
Spring Boot REST API
   │
   ▼
Processing Service
   │
   ├── FFmpeg Containers
   │
   ├── PostgreSQL
   │
   └── AWS S3
```
---
## Engineering Challenges

- Managed long-running video processing jobs asynchronously
- Generated temporary presigned URLs for secure asset delivery
- Coordinated FFmpeg container execution from Spring Boot
- Tracked processing metadata and job states in PostgreSQL
- Designed a scalable pipeline architecture for future media workflows
---

## API Overview

### Process Video

```http
POST /api/v1/video/process
```

**Authentication:** Bearer JWT

**Content-Type:** multipart/form-data

| Parameter | Description |
|-----------|-------------|
| file | Uploaded video |
| action | `silence_trim`, `compress`, `audio`, `proxy` |

### Get User Videos

```http
GET /api/v1/video/my-videos
```

**Authentication:** Bearer JWT

Returns generated media URLs associated with the authenticated user.

---

## Example Processing Pipelines

| Action | Description |
|---------|-------------|
| `silence_trim` | Removes silent sections |
| `compress` | Reduces file size |
| `audio` | Extracts MP3 audio |
| `proxy` | Creates lightweight preview versions |
---
### Requirements

- Java 21
- PostgreSQL
- FFmpeg
- AWS S3 bucket
---
## Installation

### Install FFmpeg

```bash
# macOS
brew install ffmpeg

# Ubuntu/Debian
sudo apt update
sudo apt install ffmpeg
```

Verify the installation:

```bash
java --version
ffmpeg -version
```

---

## Configuration

Update `src/main/resources/application.properties`:

```properties
# PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/db
spring.datasource.username=your_postgres_user
spring.datasource.password=your_secure_password

# AWS
aws.accessKeyId=YOUR_ACCESS_KEY
aws.secretKey=YOUR_SECRET_KEY
aws.region=us-east-1
aws.s3.bucketName=transcoded-assets
```
---
## Running Tests

Unit tests use Mockito to isolate service and controller logic.

```bash
./mvnw clean test
```
