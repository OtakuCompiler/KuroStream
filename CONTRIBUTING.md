# Contributing to KuroStream

Thank you for your interest in contributing to KuroStream!

## Code of Conduct

Be respectful. Constructive criticism welcome. No harassment.

## How to Contribute

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## Development Setup

```bash
git clone https://github.com/OtakuCompiler/KuroStream-Stabilized.git
cd KuroStream-Stabilized
./gradlew assembleDebug
```

## Commit Message Convention

- `feat:` New feature
- `fix:` Bug fix
- `perf:` Performance improvement
- `refactor:` Code restructuring
- `docs:` Documentation changes
- `test:` Test additions/changes
- `chore:` Build/tooling changes

## Pull Request Checklist

- [ ] Code compiles without warnings
- [ ] All tests pass
- [ ] No new lint errors
- [ ] Arctic Fuse 3 UI guidelines followed
- [ ] Memory budget respected (<125MB for 4K+Atmos)
- [ ] No placeholder implementations
- [ ] No TODO/FIXME comments left in production code
