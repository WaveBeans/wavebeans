package io.wavebeans.metrics

class TimeseriesList<T : Any>(
        private val granularValueInMs: Long = 60000,
        private val accumulator: (T, T) -> T
) {

    @Volatile
    private var timeseries = ArrayList<Pair<Long, T>>()

    private var lastValueTimestamp: Long = -1

    private var lastValue: T? = null

    @Synchronized
    fun reset() {
        timeseries = ArrayList()
        lastValueTimestamp = -1
        lastValue = null
    }

    @Synchronized
    fun append(v: T, now: Long = System.currentTimeMillis()): Boolean {
        if (now < lastValueTimestamp) return false

        if (granularValueInMs > 0) {
            lastValue = lastValue?.let {
                val lastBucket = lastValueTimestamp / granularValueInMs
                val currentBucket = now / granularValueInMs
                if (currentBucket > lastBucket) {
                    timeseries.add(Pair(lastBucket * granularValueInMs, it))
                    v
                } else {
                    accumulator(it, v)
                }
            } ?: v
            lastValueTimestamp = now
        } else {
            timeseries.add(Pair(now, v))
        }
        return true
    }

    @Synchronized
    fun leaveOnlyLast(intervalMs: Long, now: Long = System.currentTimeMillis()) {
        val lastMarker = now - intervalMs
        timeseries.removeIf { it.first < lastMarker }
        if (lastValueTimestamp < lastMarker) {
            lastValueTimestamp = -1
            lastValue = null
        }
    }

    @Synchronized
    fun removeAllBefore(before: Long): List<Pair<Long, T>> {
        val i = timeseries.iterator()
        val removedValues = ArrayList<Pair<Long, T>>()
        while (i.hasNext()) {
            val e = i.next()
            if (e.first < before) {
                i.remove()
                removedValues += e
            } else {
                break // the timestamp is growing only, may cancel the loop
            }
        }
        if (lastValueTimestamp < before && lastValue != null) {
            removedValues += Pair(lastValueTimestamp, lastValue!!)
            lastValueTimestamp = -1
            lastValue = null
        }
        return removedValues
    }

    @Synchronized
    fun inLast(intervalMs: Long, now: Long = System.currentTimeMillis()): T? {
        val lastMarker = now - intervalMs
        val v = timeseries.asSequence()
                .dropWhile { it.first < lastMarker }
                .map { it.second }
                .reduceOrNull { acc, v -> accumulator(acc, v) }
        return when {
            lastValue != null && lastMarker < lastValueTimestamp && v != null -> accumulator(v, lastValue!!)
            v == null -> lastValue
            else -> v
        }
    }

    @Synchronized
    fun merge(values: List<Pair<Long, T>>) {
        val i = (timeseries + (
                lastValue?.let { listOf(Pair(lastValueTimestamp, it)) }
                        ?: emptyList()
                ))
                .iterator()
        val j = values.iterator()

        reset()
        var e1: Pair<Long, T>? = null
        var e2: Pair<Long, T>? = null
        while (true) {
            e1 = if (e1 == null && i.hasNext()) i.next() else e1
            e2 = if (e2 == null && j.hasNext()) j.next() else e2
            if (e1 == null && e2 == null) {
                break
            } else if (e1 != null && e2 == null) {
                append(e1.second, e1.first)
                e1 = null
            } else if (e1 == null && e2 != null) {
                append(e2.second, e2.first)
                e2 = null
            } else if (e1!!.first < e2!!.first) {
                append(e1.second, e1.first)
                e1 = null
            } else { // (e1!!.first >= e2!!.first)
                append(e2.second, e2.first)
                e2 = null
            }
        }
    }
}