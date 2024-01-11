# webcam-capture-kotlin

Kotlin port of `https://github.com/sarxos/webcam-capture`.

# How to include dependency to your project (JVM only)

1. Clone this repo
2. In the dependent project, include this library like so in `settings.gradle.kts`:
```kotlin
include("webcam-capture-kotlin")
project(":webcam-capture-kotlin").projectDir = file("C:\\Users\\kietm\\GitHub\\webcam-capture-kotlin")
```
3. In your `build.gradle.kts`, add as a dependency like so:
```
dependencies {
    // OTHER LINES OMITTED
    implementation(project(":webcam-capture-kotlin"))
}
```

# Example Usage

## Get list of available webcams

```kotlin
val webcamNames = Webcam.webcams.map { it.name }
println("Webcams available:")
webcamNames.forEachIndexed { index, s ->
    println("$index: $s")
}
```

## Get image from webcam

```kotlin
// Assuming webcam supports 720p/1080p
val nonStandardResolutions = arrayOf(
    WebcamResolution.HD.size,
    WebcamResolution.FHD.size,
)

val webcam: Webcam = Webcam.getWebcamByName("<Webcam name>")!!
webcam.customViewSizes = nonStandardResolutions
webcam.viewSize = WebcamResolution.FHD.size

val image = webcam.image
```

More examples: https://github.com/sarxos/webcam-capture?tab=readme-ov-file#more-examples

# Possible Issues

```shell
Caused by: java.lang.RuntimeException: Library 'OpenIMAJGrabber' was not loaded successfully from file 'C:\Users\User\AppData\Local\Temp\BridJExtractedLibraries890841975285135309\OpenIMAJGrabber.dll'
	at com.sparrowwallet.merged.module@1.4.0/org.bridj.BridJ.getNativeLibrary(Unknown Source)
```

To fix, have to install:

[Microsoft Visual C++ 2010 Service Pack 1 Redistributable Package MFC Security Update](https://www.microsoft.com/en-us/download/details.aspx?id=26999)

Particularly: vcredist_x64.exe

Fix was from: https://github.com/openimaj/openimaj/issues/311

# License from https://github.com/sarxos/webcam-capture

Copyright (C) 2012 - 2017 Bartosz Firyn (https://github.com/sarxos) and contributors

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
