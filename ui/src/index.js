const graphJson = "{\n" +
    "    \"refs\": [\n" +
    "        {\n" +
    "            \"id\": 17,\n" +
    "            \"type\": \"io.wavebeans.execution.BeanGroup\",\n" +
    "            \"params\": {\n" +
    "                \"type\": \"io.wavebeans.execution.BeanGroupParams\",\n" +
    "                \"beanRefs\": [\n" +
    "                    {\n" +
    "                        \"id\": 1,\n" +
    "                        \"type\": \"io.wavebeans.lib.io.CsvSampleStreamOutput\",\n" +
    "                        \"params\": {\n" +
    "                            \"type\": \"io.wavebeans.lib.io.CsvSampleStreamOutputParams\",\n" +
    "                            \"uri\": \"file:///var/folders/1n/q1rsg7_90mz903hqck3ghcvm0000gn/T/test2420156773520590563.csv\",\n" +
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
    "                            \"length\": 50,\n" +
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
    "            \"id\": 18,\n" +
    "            \"type\": \"io.wavebeans.execution.BeanGroup\",\n" +
    "            \"params\": {\n" +
    "                \"type\": \"io.wavebeans.execution.BeanGroupParams\",\n" +
    "                \"beanRefs\": [\n" +
    "                    {\n" +
    "                        \"id\": 3,\n" +
    "                        \"type\": \"io.wavebeans.lib.stream.ChangeAmplitudeSampleStream\",\n" +
    "                        \"params\": {\n" +
    "                            \"type\": \"io.wavebeans.lib.stream.ChangeAmplitudeSampleStreamParams\",\n" +
    "                            \"multiplier\": 1.0\n" +
    "                        },\n" +
    "                        \"partition\": 0\n" +
    "                    },\n" +
    "                    {\n" +
    "                        \"id\": 4,\n" +
    "                        \"type\": \"io.wavebeans.lib.stream.InfiniteSampleStream\",\n" +
    "                        \"params\": {\n" +
    "                            \"type\": \"io.wavebeans.lib.NoParams\"\n" +
    "                        },\n" +
    "                        \"partition\": 0\n" +
    "                    }\n" +
    "                ],\n" +
    "                \"links\": [\n" +
    "                    {\n" +
    "                        \"from\": 3,\n" +
    "                        \"to\": 4,\n" +
    "                        \"fromPartition\": 0,\n" +
    "                        \"toPartition\": 0,\n" +
    "                        \"order\": 0\n" +
    "                    }\n" +
    "                ]\n" +
    "            },\n" +
    "            \"partition\": 0\n" +
    "        },\n" +
    "        {\n" +
    "            \"id\": 5,\n" +
    "            \"type\": \"io.wavebeans.execution.SeqInput\",\n" +
    "            \"params\": {\n" +
    "                \"type\": \"io.wavebeans.lib.NoParams\"\n" +
    "            },\n" +
    "            \"partition\": 0\n" +
    "        },\n" +
    "        {\n" +
    "            \"id\": 18,\n" +
    "            \"type\": \"io.wavebeans.execution.BeanGroup\",\n" +
    "            \"params\": {\n" +
    "                \"type\": \"io.wavebeans.execution.BeanGroupParams\",\n" +
    "                \"beanRefs\": [\n" +
    "                    {\n" +
    "                        \"id\": 3,\n" +
    "                        \"type\": \"io.wavebeans.lib.stream.ChangeAmplitudeSampleStream\",\n" +
    "                        \"params\": {\n" +
    "                            \"type\": \"io.wavebeans.lib.stream.ChangeAmplitudeSampleStreamParams\",\n" +
    "                            \"multiplier\": 1.0\n" +
    "                        },\n" +
    "                        \"partition\": 1\n" +
    "                    },\n" +
    "                    {\n" +
    "                        \"id\": 4,\n" +
    "                        \"type\": \"io.wavebeans.lib.stream.InfiniteSampleStream\",\n" +
    "                        \"params\": {\n" +
    "                            \"type\": \"io.wavebeans.lib.NoParams\"\n" +
    "                        },\n" +
    "                        \"partition\": 1\n" +
    "                    }\n" +
    "                ],\n" +
    "                \"links\": [\n" +
    "                    {\n" +
    "                        \"from\": 3,\n" +
    "                        \"to\": 4,\n" +
    "                        \"fromPartition\": 1,\n" +
    "                        \"toPartition\": 1,\n" +
    "                        \"order\": 0\n" +
    "                    }\n" +
    "                ]\n" +
    "            },\n" +
    "            \"partition\": 1\n" +
    "        },\n" +
    "        {\n" +
    "            \"id\": 19,\n" +
    "            \"type\": \"io.wavebeans.execution.BeanGroup\",\n" +
    "            \"params\": {\n" +
    "                \"type\": \"io.wavebeans.execution.BeanGroupParams\",\n" +
    "                \"beanRefs\": [\n" +
    "                    {\n" +
    "                        \"id\": 6,\n" +
    "                        \"type\": \"io.wavebeans.lib.io.CsvSampleStreamOutput\",\n" +
    "                        \"params\": {\n" +
    "                            \"type\": \"io.wavebeans.lib.io.CsvSampleStreamOutputParams\",\n" +
    "                            \"uri\": \"file:///var/folders/1n/q1rsg7_90mz903hqck3ghcvm0000gn/T/test1521262003403646232.csv\",\n" +
    "                            \"outputTimeUnit\": \"MILLISECONDS\",\n" +
    "                            \"encoding\": \"UTF-8\"\n" +
    "                        },\n" +
    "                        \"partition\": 0\n" +
    "                    },\n" +
    "                    {\n" +
    "                        \"id\": 7,\n" +
    "                        \"type\": \"io.wavebeans.lib.stream.TrimmedFiniteSampleStream\",\n" +
    "                        \"params\": {\n" +
    "                            \"type\": \"io.wavebeans.lib.stream.TrimmedFiniteSampleStreamParams\",\n" +
    "                            \"length\": 50,\n" +
    "                            \"timeUnit\": \"MILLISECONDS\"\n" +
    "                        },\n" +
    "                        \"partition\": 0\n" +
    "                    }\n" +
    "                ],\n" +
    "                \"links\": [\n" +
    "                    {\n" +
    "                        \"from\": 6,\n" +
    "                        \"to\": 7,\n" +
    "                        \"fromPartition\": 0,\n" +
    "                        \"toPartition\": 0,\n" +
    "                        \"order\": 0\n" +
    "                    }\n" +
    "                ]\n" +
    "            },\n" +
    "            \"partition\": 0\n" +
    "        },\n" +
    "        {\n" +
    "            \"id\": 8,\n" +
    "            \"type\": \"io.wavebeans.lib.stream.MergedSampleStream\",\n" +
    "            \"params\": {\n" +
    "                \"type\": \"io.wavebeans.lib.stream.MergedSampleStreamParams\",\n" +
    "                \"shift\": 0,\n" +
    "                \"operation\": \"+\",\n" +
    "                \"start\": 0,\n" +
    "                \"end\": null,\n" +
    "                \"timeUnit\": \"MILLISECONDS\"\n" +
    "            },\n" +
    "            \"partition\": 0\n" +
    "        },\n" +
    "        {\n" +
    "            \"id\": 20,\n" +
    "            \"type\": \"io.wavebeans.execution.BeanGroup\",\n" +
    "            \"params\": {\n" +
    "                \"type\": \"io.wavebeans.execution.BeanGroupParams\",\n" +
    "                \"beanRefs\": [\n" +
    "                    {\n" +
    "                        \"id\": 9,\n" +
    "                        \"type\": \"io.wavebeans.lib.stream.ChangeAmplitudeSampleStream\",\n" +
    "                        \"params\": {\n" +
    "                            \"type\": \"io.wavebeans.lib.stream.ChangeAmplitudeSampleStreamParams\",\n" +
    "                            \"multiplier\": 0.0\n" +
    "                        },\n" +
    "                        \"partition\": 0\n" +
    "                    },\n" +
    "                    {\n" +
    "                        \"id\": 10,\n" +
    "                        \"type\": \"io.wavebeans.lib.stream.InfiniteSampleStream\",\n" +
    "                        \"params\": {\n" +
    "                            \"type\": \"io.wavebeans.lib.NoParams\"\n" +
    "                        },\n" +
    "                        \"partition\": 0\n" +
    "                    }\n" +
    "                ],\n" +
    "                \"links\": [\n" +
    "                    {\n" +
    "                        \"from\": 9,\n" +
    "                        \"to\": 10,\n" +
    "                        \"fromPartition\": 0,\n" +
    "                        \"toPartition\": 0,\n" +
    "                        \"order\": 0\n" +
    "                    }\n" +
    "                ]\n" +
    "            },\n" +
    "            \"partition\": 0\n" +
    "        },\n" +
    "        {\n" +
    "            \"id\": 11,\n" +
    "            \"type\": \"io.wavebeans.execution.SeqInput\",\n" +
    "            \"params\": {\n" +
    "                \"type\": \"io.wavebeans.lib.NoParams\"\n" +
    "            },\n" +
    "            \"partition\": 0\n" +
    "        },\n" +
    "        {\n" +
    "            \"id\": 8,\n" +
    "            \"type\": \"io.wavebeans.lib.stream.MergedSampleStream\",\n" +
    "            \"params\": {\n" +
    "                \"type\": \"io.wavebeans.lib.stream.MergedSampleStreamParams\",\n" +
    "                \"shift\": 0,\n" +
    "                \"operation\": \"+\",\n" +
    "                \"start\": 0,\n" +
    "                \"end\": null,\n" +
    "                \"timeUnit\": \"MILLISECONDS\"\n" +
    "            },\n" +
    "            \"partition\": 1\n" +
    "        },\n" +
    "        {\n" +
    "            \"id\": 20,\n" +
    "            \"type\": \"io.wavebeans.execution.BeanGroup\",\n" +
    "            \"params\": {\n" +
    "                \"type\": \"io.wavebeans.execution.BeanGroupParams\",\n" +
    "                \"beanRefs\": [\n" +
    "                    {\n" +
    "                        \"id\": 9,\n" +
    "                        \"type\": \"io.wavebeans.lib.stream.ChangeAmplitudeSampleStream\",\n" +
    "                        \"params\": {\n" +
    "                            \"type\": \"io.wavebeans.lib.stream.ChangeAmplitudeSampleStreamParams\",\n" +
    "                            \"multiplier\": 0.0\n" +
    "                        },\n" +
    "                        \"partition\": 1\n" +
    "                    },\n" +
    "                    {\n" +
    "                        \"id\": 10,\n" +
    "                        \"type\": \"io.wavebeans.lib.stream.InfiniteSampleStream\",\n" +
    "                        \"params\": {\n" +
    "                            \"type\": \"io.wavebeans.lib.NoParams\"\n" +
    "                        },\n" +
    "                        \"partition\": 1\n" +
    "                    }\n" +
    "                ],\n" +
    "                \"links\": [\n" +
    "                    {\n" +
    "                        \"from\": 9,\n" +
    "                        \"to\": 10,\n" +
    "                        \"fromPartition\": 1,\n" +
    "                        \"toPartition\": 1,\n" +
    "                        \"order\": 0\n" +
    "                    }\n" +
    "                ]\n" +
    "            },\n" +
    "            \"partition\": 1\n" +
    "        },\n" +
    "        {\n" +
    "            \"id\": 12,\n" +
    "            \"type\": \"io.wavebeans.lib.io.CsvFftStreamOutput\",\n" +
    "            \"params\": {\n" +
    "                \"type\": \"io.wavebeans.lib.io.CsvFftStreamOutputParams\",\n" +
    "                \"uri\": \"file:///var/folders/1n/q1rsg7_90mz903hqck3ghcvm0000gn/T/test2019209937713874301.csv\",\n" +
    "                \"isMagnitude\": true,\n" +
    "                \"encoding\": \"UTF-8\"\n" +
    "            },\n" +
    "            \"partition\": 0\n" +
    "        },\n" +
    "        {\n" +
    "            \"id\": 13,\n" +
    "            \"type\": \"io.wavebeans.lib.stream.fft.TrimmedFiniteFftStream\",\n" +
    "            \"params\": {\n" +
    "                \"type\": \"io.wavebeans.lib.stream.fft.TrimmedFiniteFftStreamParams\",\n" +
    "                \"length\": 50,\n" +
    "                \"timeUnit\": \"MILLISECONDS\"\n" +
    "            },\n" +
    "            \"partition\": 0\n" +
    "        },\n" +
    "        {\n" +
    "            \"id\": 14,\n" +
    "            \"type\": \"io.wavebeans.lib.stream.fft.FftStreamImpl\",\n" +
    "            \"params\": {\n" +
    "                \"type\": \"io.wavebeans.lib.stream.fft.FftStreamParams\",\n" +
    "                \"n\": 512,\n" +
    "                \"start\": 0,\n" +
    "                \"end\": null,\n" +
    "                \"timeUnit\": \"MILLISECONDS\"\n" +
    "            },\n" +
    "            \"partition\": 0\n" +
    "        },\n" +
    "        {\n" +
    "            \"id\": 15,\n" +
    "            \"type\": \"io.wavebeans.lib.stream.window.SampleWindowStreamImpl\",\n" +
    "            \"params\": {\n" +
    "                \"type\": \"io.wavebeans.lib.stream.window.WindowStreamParams\",\n" +
    "                \"windowSize\": 401,\n" +
    "                \"step\": 401,\n" +
    "                \"start\": 0,\n" +
    "                \"end\": null,\n" +
    "                \"timeUnit\": \"MILLISECONDS\"\n" +
    "            },\n" +
    "            \"partition\": 0\n" +
    "        },\n" +
    "        {\n" +
    "            \"id\": 14,\n" +
    "            \"type\": \"io.wavebeans.lib.stream.fft.FftStreamImpl\",\n" +
    "            \"params\": {\n" +
    "                \"type\": \"io.wavebeans.lib.stream.fft.FftStreamParams\",\n" +
    "                \"n\": 512,\n" +
    "                \"start\": 0,\n" +
    "                \"end\": null,\n" +
    "                \"timeUnit\": \"MILLISECONDS\"\n" +
    "            },\n" +
    "            \"partition\": 1\n" +
    "        },\n" +
    "        {\n" +
    "            \"id\": 16,\n" +
    "            \"type\": \"io.wavebeans.lib.io.CsvFftStreamOutput\",\n" +
    "            \"params\": {\n" +
    "                \"type\": \"io.wavebeans.lib.io.CsvFftStreamOutputParams\",\n" +
    "                \"uri\": \"file:///var/folders/1n/q1rsg7_90mz903hqck3ghcvm0000gn/T/test6721941998167757181.csv\",\n" +
    "                \"isMagnitude\": false,\n" +
    "                \"encoding\": \"UTF-8\"\n" +
    "            },\n" +
    "            \"partition\": 0\n" +
    "        }\n" +
    "    ],\n" +
    "    \"links\": [\n" +
    "        {\n" +
    "            \"from\": 17,\n" +
    "            \"to\": 18,\n" +
    "            \"fromPartition\": 0,\n" +
    "            \"toPartition\": 0,\n" +
    "            \"order\": 0\n" +
    "        },\n" +
    "        {\n" +
    "            \"from\": 17,\n" +
    "            \"to\": 18,\n" +
    "            \"fromPartition\": 0,\n" +
    "            \"toPartition\": 1,\n" +
    "            \"order\": 0\n" +
    "        },\n" +
    "        {\n" +
    "            \"from\": 18,\n" +
    "            \"to\": 5,\n" +
    "            \"fromPartition\": 0,\n" +
    "            \"toPartition\": 0,\n" +
    "            \"order\": 0\n" +
    "        },\n" +
    "        {\n" +
    "            \"from\": 18,\n" +
    "            \"to\": 5,\n" +
    "            \"fromPartition\": 1,\n" +
    "            \"toPartition\": 0,\n" +
    "            \"order\": 0\n" +
    "        },\n" +
    "        {\n" +
    "            \"from\": 19,\n" +
    "            \"to\": 8,\n" +
    "            \"fromPartition\": 0,\n" +
    "            \"toPartition\": 0,\n" +
    "            \"order\": 0\n" +
    "        },\n" +
    "        {\n" +
    "            \"from\": 19,\n" +
    "            \"to\": 8,\n" +
    "            \"fromPartition\": 0,\n" +
    "            \"toPartition\": 1,\n" +
    "            \"order\": 0\n" +
    "        },\n" +
    "        {\n" +
    "            \"from\": 8,\n" +
    "            \"to\": 18,\n" +
    "            \"fromPartition\": 0,\n" +
    "            \"toPartition\": 0,\n" +
    "            \"order\": 0\n" +
    "        },\n" +
    "        {\n" +
    "            \"from\": 8,\n" +
    "            \"to\": 18,\n" +
    "            \"fromPartition\": 1,\n" +
    "            \"toPartition\": 1,\n" +
    "            \"order\": 0\n" +
    "        },\n" +
    "        {\n" +
    "            \"from\": 8,\n" +
    "            \"to\": 20,\n" +
    "            \"fromPartition\": 0,\n" +
    "            \"toPartition\": 0,\n" +
    "            \"order\": 1\n" +
    "        },\n" +
    "        {\n" +
    "            \"from\": 8,\n" +
    "            \"to\": 20,\n" +
    "            \"fromPartition\": 1,\n" +
    "            \"toPartition\": 1,\n" +
    "            \"order\": 1\n" +
    "        },\n" +
    "        {\n" +
    "            \"from\": 20,\n" +
    "            \"to\": 11,\n" +
    "            \"fromPartition\": 0,\n" +
    "            \"toPartition\": 0,\n" +
    "            \"order\": 0\n" +
    "        },\n" +
    "        {\n" +
    "            \"from\": 20,\n" +
    "            \"to\": 11,\n" +
    "            \"fromPartition\": 1,\n" +
    "            \"toPartition\": 0,\n" +
    "            \"order\": 0\n" +
    "        },\n" +
    "        {\n" +
    "            \"from\": 12,\n" +
    "            \"to\": 13,\n" +
    "            \"fromPartition\": 0,\n" +
    "            \"toPartition\": 0,\n" +
    "            \"order\": 0\n" +
    "        },\n" +
    "        {\n" +
    "            \"from\": 13,\n" +
    "            \"to\": 14,\n" +
    "            \"fromPartition\": 0,\n" +
    "            \"toPartition\": 0,\n" +
    "            \"order\": 0\n" +
    "        },\n" +
    "        {\n" +
    "            \"from\": 13,\n" +
    "            \"to\": 14,\n" +
    "            \"fromPartition\": 0,\n" +
    "            \"toPartition\": 1,\n" +
    "            \"order\": 0\n" +
    "        },\n" +
    "        {\n" +
    "            \"from\": 14,\n" +
    "            \"to\": 15,\n" +
    "            \"fromPartition\": 0,\n" +
    "            \"toPartition\": 0,\n" +
    "            \"order\": 0\n" +
    "        },\n" +
    "        {\n" +
    "            \"from\": 14,\n" +
    "            \"to\": 15,\n" +
    "            \"fromPartition\": 1,\n" +
    "            \"toPartition\": 0,\n" +
    "            \"order\": 0\n" +
    "        },\n" +
    "        {\n" +
    "            \"from\": 15,\n" +
    "            \"to\": 8,\n" +
    "            \"fromPartition\": 0,\n" +
    "            \"toPartition\": 0,\n" +
    "            \"order\": 0\n" +
    "        },\n" +
    "        {\n" +
    "            \"from\": 15,\n" +
    "            \"to\": 8,\n" +
    "            \"fromPartition\": 0,\n" +
    "            \"toPartition\": 1,\n" +
    "            \"order\": 0\n" +
    "        },\n" +
    "        {\n" +
    "            \"from\": 16,\n" +
    "            \"to\": 13,\n" +
    "            \"fromPartition\": 0,\n" +
    "            \"toPartition\": 0,\n" +
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
    let renderer = new Dracula.Renderer.Raphael('#paper', graph, 1600, 1200);
    renderer.draw()
});


