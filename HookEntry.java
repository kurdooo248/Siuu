package com.mt5.hook;

import android.view.View;
import android.widget.TextView;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEntry implements IXposedHookLoadPackage {
    private static final String TARGET_PKG = "net.metaquotes.metatrader5";
    private static final int TAG_KEY = 0x7f0abcde;
    private static final String TAG_DEPOSIT = "MT5_TAG_DEPOSIT";
    private static final String TAG_WITHDRAW = "MT5_TAG_WITHDRAW";

    private CharSequence patchText(CharSequence in) {
        if (in == null) return null;
        String s = in.toString();
        return s.replace("حساب تجريبي","حساب حقيقي")
                .replace("تجريبي","حقيقي")
                .replace("DEMO","LIVE")
                .replace("Demo","Live")
                .replace("demo","live");
    }
    private boolean isDepOrWdr(CharSequence cs){
        if(cs==null) return false;
        String t=cs.toString().trim();
        return t.equalsIgnoreCase("Deposit")||t.equalsIgnoreCase("Withdraw")||t.contains("إيداع")||t.contains("سحب");
    }
    private static boolean hasTag(View v){
        Object tag=v.getTag(TAG_KEY);
        return TAG_DEPOSIT.equals(tag)||TAG_WITHDRAW.equals(tag);
    }
    private static void forceEnable(View v){
        try{
            if(!v.isEnabled()) v.setEnabled(true);
            if(!v.isClickable()) v.setClickable(true);
            if(v.getVisibility()!=View.VISIBLE) v.setVisibility(View.VISIBLE);
            if(v.getAlpha()<1f) v.setAlpha(1f);
        }catch(Throwable ignore){}
    }

    @Override public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam l) throws Throwable {
        if(!l.packageName.equals(TARGET_PKG)) return;

        XposedHelpers.findAndHookMethod(TextView.class,"setText",CharSequence.class,new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam p){
                CharSequence in=(CharSequence)p.args[0];
                CharSequence out=patchText(in);
                if(out!=null && !out.equals(in)) p.args[0]=out;

                View v=(View)p.thisObject;
                CharSequence txt=(CharSequence)p.args[0];
                if(isDepOrWdr(txt)){
                    String tag=txt.toString().contains("سحب")||txt.toString().equalsIgnoreCase("Withdraw")?TAG_WITHDRAW:TAG_DEPOSIT;
                    v.setTag(TAG_KEY,tag);
                    forceEnable(v);
                }
            }
            @Override protected void afterHookedMethod(MethodHookParam p){
                View v=(View)p.thisObject;
                if(v instanceof TextView){
                    CharSequence txt=((TextView)v).getText();
                    if(isDepOrWdr(txt)){
                        String tag=txt.toString().contains("سحب")||txt.toString().equalsIgnoreCase("Withdraw")?TAG_WITHDRAW:TAG_DEPOSIT;
                        v.setTag(TAG_KEY,tag);
                        forceEnable(v);
                    }
                }
            }
        });

        XposedHelpers.findAndHookMethod(View.class,"setEnabled",boolean.class,new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam p){
                View v=(View)p.thisObject;
                if(hasTag(v) && !(Boolean)p.args[0]) p.args[0]=true;
            }
        });
        XposedHelpers.findAndHookMethod(View.class,"setClickable",boolean.class,new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam p){
                View v=(View)p.thisObject;
                if(hasTag(v) && !(Boolean)p.args[0]) p.args[0]=true;
            }
        });
        XposedHelpers.findAndHookMethod(View.class,"setVisibility",int.class,new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam p){
                View v=(View)p.thisObject;
                if(hasTag(v)) p.args[0]=View.VISIBLE;
            }
        });
        XposedHelpers.findAndHookMethod(View.class,"setAlpha",float.class,new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam p){
                View v=(View)p.thisObject;
                if(hasTag(v) && ((float)p.args[0])<1f) p.args[0]=1f;
            }
        });
    }
}
