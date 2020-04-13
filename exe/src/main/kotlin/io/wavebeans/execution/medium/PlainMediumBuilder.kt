package io.wavebeans.execution.medium

class PlainMediumBuilder : MediumBuilder {
    override fun from(objects: List<Any>): Medium = PlainMedium(objects)
}