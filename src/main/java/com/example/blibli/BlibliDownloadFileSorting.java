package com.example.blibli;


import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.FileUtils;
import org.springframework.util.StringUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @Date 2020/11/14 20:42
 * @Auther lw
 * @Description windows B站UWP客户端下载文件整理，包含文件重命名并提取到同一文件夹下
 *
 * 需要的组件
 * gson
 * ffmpeg，将该软件安装到操作系统上，用于对切割后的视频文件进行的合并
 * commons-io，方便对文件的处理
 *
 * 使用cmd窗口中使用ffmpeg -version测试ffmpeg是否安装成功
 *
 * 处理流程
 *  对不需要合并的直接复制文件到指定文件夹
 *  对需要合并的
 *           1）核心是利用ffmpeg进行视频转换，我们自己并不写转换视频的代码，只是调用ffmpeg，它会帮我们完成视频的转换。
 *              ffmpeg支持的类型有：asx，asf，mpg，wmv，3gp，mp4，mov，avi，flv等，这些类型，可以利用ffmpeg进行直接转换。
 *              ffmpeg不支持的类型有：wmv9，rm，rmvb等，这些类型需要先用别的工具（mencoder）转换为avi(ffmpeg能解析的)格式。
 *           2）了解Java如何调用外部程序，这会是最困难的，也会是坑最多的地方。
 *           3）根据我们的需求设置ffmpeg的参数
 *
 * merge.txt中给出文件的绝对路径
 * ffmpeg使用系统中的绝对路径，输出位置也是绝对路径
 * 由于是在idea中执行，操作系统中设置的ffmpeg的环境变量是失效的，必须定位到ffmpeg.exe程序的位置才可以执行
 *
 * 报[concat @ 000001def1aa8ec0]Unsafe file name ‘C:\text\data\20190423_161512_000000.mp4’ C:\test\data\filelist.txt:Operation not permitted
 * 添加该参数解决：safe -0
 *
 *  合并时执行的命令
 * E:\ffmpeg\bin\ffmpeg -f concat -safe 0 -i C:\Users\lw\Desktop\26993050\34\merge.txt -c copy C:\Users\lw\Desktop\26993050\34\34_尚硅谷_Docker_CentOS7安装Docker（补充知识）.flv
 */
public class BlibliDownloadFileSorting {

    /**
     * FFmpeg程序执行路径
     * 当前系统安装好ffmpeg程序并配置好相应的环境变量后，值为ffmpeg可执行程序文件在实际系统中的绝对路径
     */
    private static String FFMPEG_PATH = "E:\\ffmpeg\\bin\\ffmpeg";

    private static JsonParser jsonParser = new JsonParser();

    //此处是要输出将B站下载路径下所有的视频输出到该路径，只需要给出总的文件夹，不需要为每个单独的专辑创建文件夹
    //此文件夹需要提前创建好
    private static String destDir = "F:\\B站视频整理\\";

    public static void main(String[] args) throws Exception {
        //此处的路径是B站UWP客户端中设置的路径
        File blibliDownloadDir = new File("D:\\bilibiliDownload");
        File[] blibliDownloadDirUnderAllFile = blibliDownloadDir.listFiles();
        for (File fileDir : blibliDownloadDirUnderAllFile) {
            if (fileDir.isDirectory()) {
                //对每一个专辑进行处理
                oneCourseDeal(fileDir, destDir);
            }
        }
    }

    public static void oneCourseDeal(File oneCourse, String destDir) throws Exception {
        //获取单一课程下的所有文件夹
        File[] oneCourseAllFileDir = oneCourse.listFiles();

        //遍历一个课程下的所有文件夹
        //1,2,3,4
        for(File episodeDir : oneCourseAllFileDir){
            if(episodeDir.isDirectory()) {
                //获取当前集目录下的所有文件
                File[] episodeDirUnderAllFile = episodeDir.listFiles();
                //获取当前集目录下所有视频文件
                AtomicInteger fileNum = new AtomicInteger();
                AtomicLong fileSize = new AtomicLong();
                List<String> pathList = new ArrayList<>();
                String title = "";
                String mergeFileName = "out";
                String fileFormat = "";
                for (File file : episodeDirUnderAllFile) {
                    String name = file.getName();
                    if (name.endsWith(".info")) {
                        String content = FileUtils.readFileToString(file, "UTF-8");
                        JsonObject jsonObject = jsonParser.parse(content).getAsJsonObject();
                        JsonElement partName = jsonObject.get("PartName");
                        title = jsonObject.get("Title").getAsString();
                        //当该课程不是分P的文件，而是单个视频时，会出现没有PartName的情况,此时title就是文件名称
                        mergeFileName = partName.isJsonNull() ? title : partName.getAsString();
                    } else if (name.endsWith(".mp4") || name.endsWith(".flv")) {
                        if (StringUtils.isEmpty(fileFormat)) {
                            fileFormat = name.substring(name.indexOf("."));
                        }
                        fileNum.getAndIncrement();
                        fileSize.addAndGet(FileUtils.sizeOf(file));
                        String temp = "file " + "'" + file.getCanonicalPath() + "'";
                        pathList.add(temp);
                    }
                }
                //在最后添加当前集文件夹的路径
                pathList.add(episodeDir.getCanonicalPath());
                String size = FileUtils.byteCountToDisplaySize(fileSize.get());
                System.out.println("当前集目录编号是：" + episodeDir.getName());
                System.out.println("当前集目录下视频文件的个数是：" + fileNum);
                System.out.println("当前集目录下所有视频文件大小是：" + size);
                //处理一集文件夹下有多个未合并的视频文件, 需要在系统上按照ffmpeg, 借助操作系统安装的ffmpeg软件，java调用操作系统命令行来实现文件合并
                File temp = new File(destDir + title);
                if (!temp.exists() && !temp.isDirectory()) {
                    temp.mkdir();
                }
                String renameFilePath = temp.getCanonicalPath() + "\\" + mergeFileName + fileFormat;
                System.out.println(renameFilePath);
                if (fileNum.get() > 1) {
                    //生成一个TXT文件记录要合并的视频文件名，用于ffmpeg合并时调用
                    File videoFileName = new File(pathList.get(fileNum.get()) + "\\merge.txt");
                    pathList.remove(fileNum.get());
                    FileUtils.writeStringToFile(videoFileName, "", "UTF-8", false);
                    FileUtils.writeLines(videoFileName, pathList, true);

                    //使用命令行调用系统中的ffmpeg合并文件
                    List<String> command = new ArrayList<>();
                    command.add("cmd.exe /c ");
                    command.add(FFMPEG_PATH);
//                    command.add(" -version");
                    command.add(" -f concat -safe 0 -i ");
                    command.add(videoFileName.getCanonicalPath());
                    command.add(" -c copy ");
                    command.add(renameFilePath);

                    StringBuilder sb = new StringBuilder();
                    for (String s : command) {
                        sb.append(s);
                    }
                    System.out.println("待执行的FFmpeg指令为：" + sb.toString());
                    exec(sb.toString());
                } else {
                    //不需要合并，直接重名名移动文件即可
                    for (File file : episodeDirUnderAllFile) {
                        String name = file.getName();
                        if (name.endsWith(".mp4") || name.endsWith(".flv")) {
                            File renameFile = new File(renameFilePath);
                            FileUtils.copyFile(file, renameFile);
                        }
                    }
                }
            }
        }
    }


    //使用CMD执行命令
    public static void exec(String command) {
        Runtime run = Runtime.getRuntime();
        try {
            java.lang.Process process = run.exec("cmd.exe /c" + command);
            new PrintStream(process.getErrorStream()).start();
            new PrintStream(process.getInputStream()).start();
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
