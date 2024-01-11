# webcam-capture-kotlin

Kotlin port of `https://github.com/sarxos/webcam-capture`.

# How to use

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

# License from https://github.com/sarxos/webcam-capture

Copyright (C) 2012 - 2017 Bartosz Firyn (https://github.com/sarxos) and contributors

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
