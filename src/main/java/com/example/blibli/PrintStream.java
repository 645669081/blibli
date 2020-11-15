package com.example.blibli;

/**
 * @Date 2020/11/15 16:26
 * @Auther 梁伟
 * @Description
 */
public class PrintStream extends Thread {
    java.io.InputStream __is = null;
    public PrintStream(java.io.InputStream is)
    {
        __is = is;
    }

    public void run() {
        try {
            while(this != null) {
//                InputStreamReader reader = new InputStreamReader(__is, "gbk");
//                BufferedReader br = new BufferedReader(reader);
//                StringBuffer sb = new StringBuffer();
//                String message;
//                while(StringUtils.isNotEmpty((message = br.readLine()))) {
//                    sb.append(message);
//                }
//                if (StringUtils.isNotEmpty(sb.toString().trim())) {
//                    System.out.println(sb.toString().trim());
//                }

                int _ch = __is.read();
                if(_ch != -1)
                    System.out.print((char)_ch);
                else break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
