# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |
| < 1.0   | :x:                |

## Reporting a Vulnerability

Please report security vulnerabilities to **mystical1309@gmail.com**.

Do NOT open public issues for security bugs.

## Security Features

- Certificate pinning for API endpoints
- Encrypted Proto DataStore for sensitive settings
- Plugin sandbox with ClassLoader isolation
- Extension signature verification
- Network Security Config with cleartext disabled in release
- ProGuard/R8 obfuscation
