# Android Toolkit

Commonly used set of abstractions, implementations and tools.

## How to use

- `cd` into your Android project's root and execute:

```bash
git submodule add "GIT_REPO_URL" ./Toolkit  
```

- Add the following to your `settings.gradle`:

```groovy
include ':Toolkit:Main'
include ':Toolkit:Test'
include ':Toolkit:RootTools'
include ':Toolkit:RootShell'
include ':Toolkit:CircleImageView'
include ':Toolkit:FastscrollerAlphabet'
```

- Add the following to your `build.gradle`:

```groovy
dependencies {

    api project(":Toolkit:Main")
    api project(":Toolkit:Access")
    api project(":Toolkit:RootShell")
    api project(":Toolkit:RootTools")
    api project(":Toolkit:CircleImageView")
    api project(":Toolkit:FastscrollerAlphabet")
}
```

That is all. You are ready to use the Toolkit!