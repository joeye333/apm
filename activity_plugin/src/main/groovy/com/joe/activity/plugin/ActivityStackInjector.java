package com.joe.activity.plugin;

import com.android.tools.r8.utils.F;

import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

class ActivityStackInjector {

    private List<String> mClassNameList;

    public ActivityStackInjector(List<String> classNameList){
        mClassNameList = classNameList;
    }

    public void execute(){
        Print.println("ActivityStackInjector execute started...");

        File srcFile = ScanUtils.FILE_CONTAINS_INIT_CLASS;

        if(srcFile == null){
            return;
        }

        File optJar = new File(srcFile.getParent(), srcFile.getName() + ".opt");
        if (optJar.exists()) {
            optJar.delete();
        }

        try {
            JarFile file = new JarFile(srcFile);
            Enumeration<JarEntry> enumeration = file.entries();
            JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(optJar));
            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = enumeration.nextElement();
                String entryName = jarEntry.getName();
                ZipEntry zipEntry = new ZipEntry(entryName);
                InputStream inputStream = file.getInputStream(jarEntry);
                jarOutputStream.putNextEntry(zipEntry);

                Print.println("ActivityStackInjector execute entry name:" + entryName);

                if (ScanUtils.REGISTER_CLASS_FILE_NAME.equals(entryName)){
                    ClassReader classReader = new ClassReader(inputStream);
                    // 构建一个ClassWriter对象，并设置让系统自动计算栈和本地变量大小
                    ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                    ClassVisitor classVisitor = new ActivityStackClassVisitor(classWriter);
                    //开始扫描class文件
                    classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES);

                    byte[] bytes = classWriter.toByteArray();
                    //将注入过字节码的class，写入临时jar文件里
                    jarOutputStream.write(bytes);

                } else {
                    jarOutputStream.write(IOUtils.toByteArray(inputStream));
                }

                inputStream.close();
                jarOutputStream.closeEntry();
            }

            jarOutputStream.close();
            file.close();

        } catch (IOException e) {
            e.printStackTrace();
        }


        if(srcFile.exists()){
            srcFile.delete();
        }

        optJar.renameTo(srcFile);

        Print.println("ActivityStackInjector execute end...");
    }

    private class ActivityStackClassVisitor extends ClassVisitor {

        public ActivityStackClassVisitor(ClassVisitor classVisitor) {
            super(Opcodes.ASM5, classVisitor);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {


            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

            Print.println("ActivityStackInjector ActivityStackClassVisitor visitMethod name:" + name);
            if ("loadComponentActivityStack".equals(name)) {
                mv = new LoadComponentActivityStack(mv, access, name, descriptor);
            }
            return mv;
        }
    }

    private class LoadComponentActivityStack extends AdviceAdapter{

        protected LoadComponentActivityStack(MethodVisitor methodVisitor, int access, String name, String descriptor) {
            super(Opcodes.ASM5, methodVisitor, access, name, descriptor);
        }

        @Override
        protected void onMethodEnter() {
            super.onMethodEnter();
            mClassNameList.forEach(new Consumer<String>() {
                @Override
                public void accept(String className) {

                    Print.println("ActivityStackInjector LoadComponentActivityStack onMethodEnter name:" + className);

                    String fullName = ScanUtils.COMPILE_FILE_PATH.replace("/", ".") + "." + className.substring(0, className.length() - 6);

                    mv.visitLdcInsn(fullName);
                    mv.visitMethodInsn(INVOKESTATIC, "cn/soonest/exmoo/common/base/stack/ActivityStackCreator",
                            "registerAppLike", "(Ljava/lang/String;)V", false);
                }
            });
            mv.visitLdcInsn("");
            mv.visitMethodInsn(INVOKESTATIC, "cn/soonest/exmoo/common/base/stack/ActivityStackCreator",
                    "printLog", "(Ljava/lang/String;)V", false);
        }

        @Override
        protected void onMethodExit(int opcode) {
            super.onMethodExit(opcode);

        }
    }

}
