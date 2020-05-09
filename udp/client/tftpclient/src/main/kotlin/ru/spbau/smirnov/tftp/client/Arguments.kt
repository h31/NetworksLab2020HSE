package ru.spbau.smirnov.tftp.client

import com.beust.jcommander.Parameter

class Arguments {
    @Parameter(names = ["-h"])
    var host: String = "127.0.0.1"

    @Parameter(names = ["-r"])
    var rootPath: String = ""

    @Parameter(names = ["-p"])
    var port: Int = 69
}