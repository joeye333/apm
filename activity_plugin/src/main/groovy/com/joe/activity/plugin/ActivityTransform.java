package com.joe.activity.plugin;

import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.api.transform.TransformOutputProvider;
import com.android.build.gradle.internal.pipeline.TransformManager;
import com.android.tools.r8.v.b.P;
import com.android.utils.FileUtils;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class ActivityTransform extends Transform {
    @Override
    public String getName() {
        return ActivityTransform.class.getSimpleName();
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS;
    }

    @Override
    public Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT;
    }

    @Override
    public boolean isIncremental() {
        return true;
    }

    @Override
    public void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation);

        try {
            List<String> fileNameList = new ArrayList<>();

            Collection<TransformInput> inputs = transformInvocation.getInputs();
            inputs.forEach(new Consumer<TransformInput>() {
                @Override
                public void accept(TransformInput transformInput) {
                    transformInput.getDirectoryInputs().forEach(new Consumer<DirectoryInput>() {
                        @Override
                        public void accept(DirectoryInput directoryInput) {
                            if(directoryInput.getFile().isDirectory()){
                                listDirectory(directoryInput.getFile(), fileNameList);
                            } else {
                                if(ScanUtils.isTargetStackClass(directoryInput.getFile())){
                                    fileNameList.add(directoryInput.getFile().getName());
                                }
                            }

                            TransformOutputProvider outputProvider = transformInvocation.getOutputProvider();
                            File dest = outputProvider.getContentLocation(directoryInput.getName(), directoryInput.getContentTypes(), directoryInput.getScopes(), Format.DIRECTORY);
                            try {
                                FileUtils.copyDirectory(directoryInput.getFile(), dest);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });


                    transformInput.getJarInputs().forEach(new Consumer<JarInput>() {
                        @Override
                        public void accept(JarInput jarInput) {
                            String name = jarInput.getName();

                            //?????????class?????????????????????jar??????????????????????????????inputs?????????outputs
                            String jarName = jarInput.getName();
                            String md5 = DigestUtils.md5Hex(jarInput.getFile().getAbsolutePath());
                            if (jarName.endsWith(".jar")) {
                                jarName = jarName.substring(0, jarName.length() - 4);
                            }
                            //????????????????????????jar??????????????????????????????????????????????????????????????????????????????????????????
                            TransformOutputProvider outputProvider = transformInvocation.getOutputProvider();
                            File dest = outputProvider.getContentLocation(jarName + md5, jarInput.getContentTypes(), jarInput.getScopes(), Format.JAR);
                            if (jarInput.getFile().getAbsolutePath().endsWith(".jar")) {
                                File src = jarInput.getFile();
                                if (ScanUtils.shouldProcessPreDexJar(src.getAbsolutePath())) {
                                    List<String> list = null;
                                    try {
                                        list = ScanUtils.scanJar(src, dest);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    if (list != null) {
                                        fileNameList.addAll(list);
                                    }
                                }
                            }
                            //???????????????????????????????????????
                            try {
                                FileUtils.copyFile(jarInput.getFile(), dest);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            });

            new ActivityStackInjector(fileNameList).execute();
        } catch (Throwable e){
            System.out.println("file err:" + e.getMessage());
        }

        System.out.println("file trans end:");
    }

    private void listDirectory(File dir, List<String> fileNameList) {
        if (!dir.exists()) {
            return;
        }

        if (!dir.isDirectory()) {
            return;
        }

        File[] files = dir.listFiles();
        if (files != null && files.length > 0) {
            for (File file : files) {
                if (file.isDirectory()) {
                    System.out.println("activity stack dir name:" + file.getName());
                    listDirectory(file, fileNameList);
                } else {
                    System.out.println("activity stack name:" + file.getName());
                    if(ScanUtils.isTargetStackClass(file)){
                        fileNameList.add(file.getName());
                    }
                }
            }
        }

    }
}
