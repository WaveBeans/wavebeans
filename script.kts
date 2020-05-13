val length = 5000L//System.getenv("L")!!.toLong()

val f1 = File.createTempFile("test", ".csv").also { println("1: $it") }
val f2 = File.createTempFile("test", ".csv").also { println("2: $it") }
val f3 = File.createTempFile("test", ".csv").also { println("3: $it") }
val f4 = File.createTempFile("test", ".csv").also { println("4: $it") }

val i1 = 440.sine()
val i2 = 880.sine()

val p1 = i1.changeAmplitude(1.0).rangeProjection(-100, length)
val p2 = i2.changeAmplitude(0.5)

val o1 = p1
        .trim(length)
        .toCsv("file://${f1.absolutePath}")
val pp = p1 + p2
val o2 = pp
        .trim(length)
        .toCsv("file://${f2.absolutePath}")
val fft = pp
        .trim(length)
        .window(401)
        .fft(512)

val o3 = fft.magnitudeToCsv("file://${f3.absolutePath}")
val o4 = fft.phaseToCsv("file://${f4.absolutePath}")


o1.out()
o2.out()
o3.out()
o4.out()
