package com.joe.activity.plugin;

import com.android.build.gradle.AppExtension;
import com.android.build.gradle.LibraryExtension;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.util.Collections;

public class ActivityPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        System.out.println("aaa------");
        ActivityTransform activityTransform = new ActivityTransform();

        AppExtension appExtension = project.getExtensions().getByType(AppExtension.class);
        appExtension.registerTransform(activityTransform, Collections.EMPTY_LIST);
//        project.getExtensions().getByType(LibraryExtension.class)
//                .registerTransform(activityTransform);
    }
}
