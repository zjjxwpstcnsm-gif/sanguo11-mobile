using System;
using System.IO;
using UnityEditor;
using UnityEditor.Build;
using UnityEditor.Build.Reporting;
using UnityEditor.SceneManagement;
using UnityEngine;
using UnityEngine.SceneManagement;

public static class ExportAndroid
{
    public static void Run()
    {
        var output = Environment.GetEnvironmentVariable("U00_UNITY_EXPORT");
        if (string.IsNullOrEmpty(output)) throw new InvalidOperationException("U00_UNITY_EXPORT is required");
        EditorSettings.serializationMode = SerializationMode.ForceText;
        EditorSettings.externalVersionControl = "Visible Meta Files";

        const string scenePath = "Assets/Trial.unity";
        var scene = EditorSceneManager.NewScene(NewSceneSetup.EmptyScene, NewSceneMode.Single);
        new GameObject("Host identity U00").AddComponent<TrialEntry>();
        EditorSceneManager.SaveScene(scene, scenePath);
        AssetDatabase.SaveAssets();

        EditorUserBuildSettings.SwitchActiveBuildTarget(BuildTargetGroup.Android, BuildTarget.Android);
        EditorUserBuildSettings.exportAsGoogleAndroidProject = true;
        PlayerSettings.SetApplicationIdentifier(NamedBuildTarget.Android, "game.sanguo.mobile.dev");
        PlayerSettings.bundleVersion = "0.81.0-unity-u00-trial";
        PlayerSettings.SetScriptingBackend(NamedBuildTarget.Android, ScriptingImplementation.IL2CPP);
        PlayerSettings.Android.targetArchitectures = AndroidArchitecture.ARM64;
        PlayerSettings.Android.minSdkVersion = AndroidSdkVersions.AndroidApiLevel26;
        Directory.CreateDirectory(output);
        var report = BuildPipeline.BuildPlayer(new BuildPlayerOptions
        {
            scenes = new[] { scenePath },
            locationPathName = output,
            target = BuildTarget.Android,
            options = BuildOptions.AcceptExternalModificationsToPlayer
        });
        if (report.summary.result != BuildResult.Succeeded || !File.Exists(Path.Combine(output, "unityLibrary", "build.gradle")))
            throw new InvalidOperationException("Unity as a Library export failed: " + report.summary.result);
    }
}
