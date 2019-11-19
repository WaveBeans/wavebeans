const graphJson = "{\"refs\":[{\"id\":8,\"type\":\"mux.lib.execution.BeanGroup\",\"params\":{\"type\":\"mux.lib.execution.BeanGroupParams\",\"beanRefs\":[{\"id\":1,\"type\":\"mux.lib.stream.DevNullSampleStreamOutput\",\"params\":{\"type\":\"mux.lib.NoParams\"},\"partition\":0},{\"id\":2,\"type\":\"mux.lib.stream.TrimmedFiniteSampleStream\",\"params\":{\"type\":\"mux.lib.stream.TrimmedFiniteSampleStreamParams\",\"length\":10000,\"timeUnit\":\"MILLISECONDS\"},\"partition\":0}],\"links\":[{\"from\":1,\"to\":2,\"fromPartition\":0,\"toPartition\":0,\"order\":0}]},\"partition\":0},{\"id\":9,\"type\":\"mux.lib.execution.BeanGroup\",\"params\":{\"type\":\"mux.lib.execution.BeanGroupParams\",\"beanRefs\":[{\"id\":3,\"type\":\"mux.lib.stream.ChangeAmplitudeSampleStream\",\"params\":{\"type\":\"mux.lib.stream.ChangeAmplitudeSampleStreamParams\",\"multiplier\":1.7},\"partition\":0},{\"id\":4,\"type\":\"mux.lib.stream.InfiniteSampleStream\",\"params\":{\"type\":\"mux.lib.NoParams\"},\"partition\":0}],\"links\":[{\"from\":3,\"to\":4,\"fromPartition\":0,\"toPartition\":0,\"order\":0}]},\"partition\":0},{\"id\":5,\"type\":\"mux.lib.io.SineGeneratedInput\",\"params\":{\"type\":\"mux.lib.io.SineGeneratedInputParams\",\"frequency\":440.0,\"amplitude\":0.5,\"timeOffset\":0.0,\"time\":null},\"partition\":0},{\"id\":9,\"type\":\"mux.lib.execution.BeanGroup\",\"params\":{\"type\":\"mux.lib.execution.BeanGroupParams\",\"beanRefs\":[{\"id\":3,\"type\":\"mux.lib.stream.ChangeAmplitudeSampleStream\",\"params\":{\"type\":\"mux.lib.stream.ChangeAmplitudeSampleStreamParams\",\"multiplier\":1.7},\"partition\":1},{\"id\":4,\"type\":\"mux.lib.stream.InfiniteSampleStream\",\"params\":{\"type\":\"mux.lib.NoParams\"},\"partition\":1}],\"links\":[{\"from\":3,\"to\":4,\"fromPartition\":1,\"toPartition\":1,\"order\":0}]},\"partition\":1},{\"id\":10,\"type\":\"mux.lib.execution.BeanGroup\",\"params\":{\"type\":\"mux.lib.execution.BeanGroupParams\",\"beanRefs\":[{\"id\":6,\"type\":\"mux.lib.stream.DevNullSampleStreamOutput\",\"params\":{\"type\":\"mux.lib.NoParams\"},\"partition\":0},{\"id\":7,\"type\":\"mux.lib.stream.TrimmedFiniteSampleStream\",\"params\":{\"type\":\"mux.lib.stream.TrimmedFiniteSampleStreamParams\",\"length\":10000,\"timeUnit\":\"MILLISECONDS\"},\"partition\":0}],\"links\":[{\"from\":6,\"to\":7,\"fromPartition\":0,\"toPartition\":0,\"order\":0}]},\"partition\":0}],\"links\":[{\"from\":8,\"to\":9,\"fromPartition\":0,\"toPartition\":0,\"order\":0},{\"from\":8,\"to\":9,\"fromPartition\":0,\"toPartition\":1,\"order\":0},{\"from\":9,\"to\":5,\"fromPartition\":0,\"toPartition\":0,\"order\":0},{\"from\":9,\"to\":5,\"fromPartition\":1,\"toPartition\":0,\"order\":0},{\"from\":10,\"to\":9,\"fromPartition\":0,\"toPartition\":0,\"order\":0},{\"from\":10,\"to\":9,\"fromPartition\":0,\"toPartition\":1,\"order\":0}],\"partitionsCount\":2}";

// src/index.js
document.addEventListener("DOMContentLoaded", function (event) {
    const element1 = document.createElement('div');
    element1.id = "paper";
    document.body.appendChild(element1);


    let Dracula = require('graphdracula');


    const g = JSON.parse(graphJson);

    let Graph = Dracula.Graph;
    let graph = new Graph();

    g["refs"].forEach(e => {
        let l = "[" + e.id + ":" + e.partition + "]\n";
        if (e.type.indexOf("Group") > -1) {
            l += e.params.beanRefs.map(b => {
                return "[" + b.id + "]" + b.type
            })
                .reduce((a, v) => a + "\n" + v)
        } else {
            l += e.type
        }
        graph.addNode("[" + e.id + ":" + e.partition + "]", {label: l})
    });
    g["links"].forEach(e => {
        graph.addEdge("[" + e.from + ":" + e.fromPartition + "]", "[" + e.to + ":" + e.toPartition + "]", {directed: true})
    });

    console.log(graph.nodes)
    let layouter = new Dracula.Layout.Spring(graph);
    let renderer = new Dracula.Renderer.Raphael('#paper', graph, 1000, 800);
    renderer.draw()
});


