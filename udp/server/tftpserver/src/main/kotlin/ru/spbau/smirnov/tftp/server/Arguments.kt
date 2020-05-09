package ru.spbau.smirnov.tftp.server

import com.beust.jcommander.Parameter

class Arguments {
    @Parameter(names = ["-r"])
    var rootPath: String = ""

    @Parameter(names = ["-p"])
    var port: Int = 69
}