const graphJson = "{\n" +
    "    \"refs\": [\n" +
    "        {\n" +
    "            \"id\": 7,\n" +
    "            \"type\": \"io.wavebeans.execution.BeanGroup\",\n" +
    "            \"params\": {\n" +
    "                \"type\": \"io.wavebeans.execution.BeanGroupParams\",\n" +
    "                \"beanRefs\": [\n" +
    "                    {\n" +
    "                        \"id\": 1,\n" +
    "                        \"type\": \"io.wavebeans.lib.io.CsvSampleStreamOutput\",\n" +
    "                        \"params\": {\n" +
    "                            \"type\": \"io.wavebeans.lib.io.CsvSampleStreamOutputParams\",\n" +
    "                            \"uri\": \"file:///var/folders/1n/q1rsg7_90mz903hqck3ghcvm0000gn/T/test2055754556937715382.csv\",\n" +
    "                            \"outputTimeUnit\": \"MILLISECONDS\",\n" +
    "                            \"encoding\": \"UTF-8\"\n" +
    "                        },\n" +
    "                        \"partition\": 0\n" +
    "                    },\n" +
    "                    {\n" +
    "                        \"id\": 2,\n" +
    "                        \"type\": \"io.wavebeans.lib.stream.TrimmedFiniteSampleStream\",\n" +
    "                        \"params\": {\n" +
    "                            \"type\": \"io.wavebeans.lib.stream.TrimmedFiniteSampleStreamParams\",\n" +
    "                            \"length\": 10,\n" +
    "                            \"timeUnit\": \"MILLISECONDS\"\n" +
    "                        },\n" +
    "                        \"partition\": 0\n" +
    "                    }\n" +
    "                ],\n" +
    "                \"links\": [\n" +
    "                    {\n" +
    "                        \"from\": 1,\n" +
    "                        \"to\": 2,\n" +
    "                        \"fromPartition\": 0,\n" +
    "                        \"toPartition\": 0,\n" +
    "                        \"order\": 0\n" +
    "                    }\n" +
    "                ]\n" +
    "            },\n" +
    "            \"partition\": 0\n" +
    "        },\n" +
    "        {\n" +
    "            \"id\": 3,\n" +
    "            \"type\": \"io.wavebeans.lib.stream.InfiniteSampleStream\",\n" +
    "            \"params\": {\n" +
    "                \"type\": \"io.wavebeans.lib.NoParams\"\n" +
    "            },\n" +
    "            \"partition\": 0\n" +
    "        },\n" +
    "        {\n" +
    "            \"id\": 4,\n" +
    "            \"type\": \"io.wavebeans.execution.SeqInput\",\n" +
    "            \"params\": {\n" +
    "                \"type\": \"io.wavebeans.lib.NoParams\"\n" +
    "            },\n" +
    "            \"partition\": 0\n" +
    "        },\n" +
    "        {\n" +
    "            \"id\": 3,\n" +
    "            \"type\": \"io.wavebeans.lib.stream.InfiniteSampleStream\",\n" +
    "            \"params\": {\n" +
    "                \"type\": \"io.wavebeans.lib.NoParams\"\n" +
    "            },\n" +
    "            \"partition\": 1\n" +
    "        },\n" +
    "        {\n" +
    "            \"id\": 8,\n" +
    "            \"type\": \"io.wavebeans.execution.BeanGroup\",\n" +
    "            \"params\": {\n" +
    "                \"type\": \"io.wavebeans.execution.BeanGroupParams\",\n" +
    "                \"beanRefs\": [\n" +
    "                    {\n" +
    "                        \"id\": 5,\n" +
    "                        \"type\": \"io.wavebeans.lib.io.CsvSampleStreamOutput\",\n" +
    "                        \"params\": {\n" +
    "                            \"type\": \"io.wavebeans.lib.io.CsvSampleStreamOutputParams\",\n" +
    "                            \"uri\": \"file:///var/folders/1n/q1rsg7_90mz903hqck3ghcvm0000gn/T/test7051186263028819371.csv\",\n" +
    "                            \"outputTimeUnit\": \"MILLISECONDS\",\n" +
    "                            \"encoding\": \"UTF-8\"\n" +
    "                        },\n" +
    "                        \"partition\": 0\n" +
    "                    },\n" +
    "                    {\n" +
    "                        \"id\": 6,\n" +
    "                        \"type\": \"io.wavebeans.lib.stream.TrimmedFiniteSampleStream\",\n" +
    "                        \"params\": {\n" +
    "                            \"type\": \"io.wavebeans.lib.stream.TrimmedFiniteSampleStreamParams\",\n" +
    "                            \"length\": 10,\n" +
    "                            \"timeUnit\": \"MILLISECONDS\"\n" +
    "                        },\n" +
    "                        \"partition\": 0\n" +
    "                    }\n" +
    "                ],\n" +
    "                \"links\": [\n" +
    "                    {\n" +
    "                        \"from\": 5,\n" +
    "                        \"to\": 6,\n" +
    "                        \"fromPartition\": 0,\n" +
    "                        \"toPartition\": 0,\n" +
    "                        \"order\": 0\n" +
    "                    }\n" +
    "                ]\n" +
    "            },\n" +
    "            \"partition\": 0\n" +
    "        }\n" +
    "    ],\n" +
    "    \"links\": [\n" +
    "        {\n" +
    "            \"from\": 7,\n" +
    "            \"to\": 3,\n" +
    "            \"fromPartition\": 0,\n" +
    "            \"toPartition\": 0,\n" +
    "            \"order\": 0\n" +
    "        },\n" +
    "        {\n" +
    "            \"from\": 7,\n" +
    "            \"to\": 3,\n" +
    "            \"fromPartition\": 0,\n" +
    "            \"toPartition\": 1,\n" +
    "            \"order\": 0\n" +
    "        },\n" +
    "        {\n" +
    "            \"from\": 3,\n" +
    "            \"to\": 4,\n" +
    "            \"fromPartition\": 0,\n" +
    "            \"toPartition\": 0,\n" +
    "            \"order\": 0\n" +
    "        },\n" +
    "        {\n" +
    "            \"from\": 3,\n" +
    "            \"to\": 4,\n" +
    "            \"fromPartition\": 1,\n" +
    "            \"toPartition\": 0,\n" +
    "            \"order\": 0\n" +
    "        },\n" +
    "        {\n" +
    "            \"from\": 8,\n" +
    "            \"to\": 3,\n" +
    "            \"fromPartition\": 0,\n" +
    "            \"toPartition\": 0,\n" +
    "            \"order\": 0\n" +
    "        },\n" +
    "        {\n" +
    "            \"from\": 8,\n" +
    "            \"to\": 3,\n" +
    "            \"fromPartition\": 0,\n" +
    "            \"toPartition\": 1,\n" +
    "            \"order\": 0\n" +
    "        }\n" +
    "    ],\n" +
    "    \"partitionsCount\": 2\n" +
    "}";

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


