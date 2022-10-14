package netty;

import model.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
public class FileHandler extends SimpleChannelInboundHandler<CloudMessage> {

    private Path serverDir;
    private Path serverDirSys;
    FileSplit fileSplit = new FileSplit();

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        serverDir = Path.of("server_files");
        serverDirSys = Path.of("server_files/system");

        File sd = serverDir.toFile();
        File sds = serverDirSys.toFile();

        if(!sd.exists()){
            Files.createDirectory(serverDir);
        }

        if(!sds.exists()){
            Files.createDirectory(serverDirSys);
        }
        ctx.writeAndFlush(new ListMessage(serverDir));

        List<String> users = SqlClient.getUsers();
        System.out.println("hhh" + users);
        ctx.writeAndFlush(new FileUsers(users));

    }



    @Override
    protected void channelRead0(ChannelHandlerContext ctx, CloudMessage cloudMessage) throws Exception {
        log.debug("Received: {}", cloudMessage.getType());

        if (cloudMessage instanceof FileMessage fileMessage) {
            Files.write(serverDir.resolve(fileMessage.getFileName()), fileMessage.getBytes());
            ctx.writeAndFlush(new ListMessage(serverDir));

        } else if (cloudMessage instanceof EnterDir enterDir) {

            String login = enterDir.getLogin();
            String pass = enterDir.getPass();
            String dir = enterDir.getFileName();

            String nickname = SqlClient.getNick(login, pass);
            if (nickname == null) {
                log.debug("Invalid login attempt " + login);
                //client.authFail();
                ctx.close();
                return;
            }


            if (String.valueOf(dir).equals(String.valueOf("system"))) {
                    log.debug("Invalid login attempt " + login);
                    //client.authFail();
                    ctx.close();
                    return;
            }



            ctx.writeAndFlush(new ListMessage(Path.of(serverDir + "/" + enterDir.getFileName())));
            serverDir = Path.of(serverDir + "/" + enterDir.getFileName());


        } else if (cloudMessage instanceof FileLastChank fileLastChank) {
            //String path = String.valueOf(Path.of(String.valueOf(serverDir)).resolve(fileLastChank.getFileName()));
            String path = String.valueOf(serverDir.resolve(fileLastChank.getFileName()));
            List<File> listOfFilesToMerge = fileSplit.listOfFilesToMerge(new File(path+".001"));
            fileSplit.mergeFiles(listOfFilesToMerge, new File(path));
            ctx.writeAndFlush(new ListMessage(serverDir));

        } else if (cloudMessage instanceof FileAuth fileAuth) {

            String login = fileAuth.getLogin();
            String pass = fileAuth.getPass();

            String nickname = SqlClient.getNick(login, pass);
            if (nickname == null) {
                log.debug("Invalid login attempt " + login);
                //client.authFail();
                ctx.close();
                return;
            }

            Path ud = Path.of(serverDirSys +"/"+ login);
            if(!ud.toFile().exists()){
                Files.createDirectory(ud);
            }

            ctx.writeAndFlush(new ListMessage(serverDir));




        } else if (cloudMessage instanceof FileRequest fileRequest) {
            String path = String.valueOf(serverDir.resolve(fileRequest.getFileName()));
            fileSplit.splitFile(new File(path));
            List<File> listOfFilesToMerge = fileSplit.listOfFilesToMerge(new File(path+".001"));
            for (File f : listOfFilesToMerge) {
                ctx.writeAndFlush(new FileMessage(Path.of(String.valueOf(f))));
            }
            fileSplit.mergeFiles(listOfFilesToMerge, new File(path));
            ctx.writeAndFlush(new FileLastChank(((FileRequest) cloudMessage).getFileName()));

        }


    }
}
