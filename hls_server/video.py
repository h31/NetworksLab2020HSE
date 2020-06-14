import os
import subprocess as sp 

def cut_file(filename, params, path="."):
    basename = os.path.basename(filename)
    name = os.path.splitext(basename)[0]
    
    n = len(params)
    
    maps = "-map 0:0 -map 0:1 " * n
    
    process = [
    "ffmpeg -y -i {}".format(filename), 
    "-preset slow -g 48 -sc_threshold 0",
    maps]
    
    stream_map = ""
    for i in range(n):
        b = params[i]
        process.append("-c:v:{0} libx264 -b:v:{0} {1}K".format(i, b))
        stream_map += "v:{0},a:{0} ".format(i)

    process.append("-c:a copy")
    process.append("-var_stream_map '{}'".format(stream_map))
    process.append("-master_pl_name {}.m3u8".format(name))
    process.append("-f hls -hls_time 6 -hls_list_size 0")
    process.append('-hls_segment_filename "{1}/{0}_v%v/{0}_segment%d.ts"'.format(name, path))
    process.append('{1}/{0}_v%v/{0}.m3u8'.format(name, path))

    result = sp.run(' '.join(process), shell=True)
    return result.returncode
