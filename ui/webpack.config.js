// var path = require('path');
var webpack = require('webpack');

// webpack.config.js
module.exports = {
    entry: [
        './src/index.js',
        './src/index.css'
    ],
    output: {
        path: __dirname,
        publicPath: '/',
        filename: 'bundle.js'
    },
    devServer: {
        port: 8080,
        host: '0.0.0.0',
        disableHostCheck: true
    },
    module: {
        rules: [
            {
                test: /\.(js)$/,
                exclude: /node_modules/,
                use: ['babel-loader']
            },
            {
                test: /\.css$/,
                use: [
                    {
                        loader: "style-loader"
                    },
                    {
                        loader: "css-loader",
                        options: {}
                    }
                ]
            }
        ]
    },
    devtool: 'source-map'
};