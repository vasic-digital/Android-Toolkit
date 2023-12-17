# Android Toolkit

Commonly used set of abstractions, implementations and tools.

## How to use

- `cd` into your Android project's root and execute:

```bash
git submodule add "GIT_REPO_URL" ./Toolkit  
```

- Add the following to your `settings.gradle`:

```groovy
include ':Toolkit:Core'
include ':Toolkit:Test'
include ':Toolkit:Access'
include ':Toolkit:RootTools'
include ':Toolkit:RootShell'
```

- Add the following to your `build.gradle`:

```groovy
dependencies {
    implementation project(':Toolkit:Core')
    implementation project(':Toolkit:Test')
    implementation project(':Toolkit:Access')
    implementation project(':Toolkit:RootTools')
    implementation project(':Toolkit:RootShell')
}
```

That is all. You are ready to use the Toolkit!