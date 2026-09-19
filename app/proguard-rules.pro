# GSY instantiates its player implementation through PlayerFactory and IJK binds
# Java classes/methods from native code. The published AARs do not include the
# consumer rules required to keep those entry points in a minified build.
-keepattributes *Annotation*
-keep class com.shuyu.gsyvideoplayer.player.** { *; }
-keep class tv.danmaku.ijk.media.player.** { *; }

# This view is inflated by its fully qualified name from feature:player XML.
-keep public class com.aa.duanju.feature.player.AAVideoPlayer {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
}
