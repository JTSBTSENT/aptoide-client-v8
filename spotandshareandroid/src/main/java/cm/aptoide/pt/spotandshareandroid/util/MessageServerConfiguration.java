package cm.aptoide.pt.spotandshareandroid.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.support.annotation.NonNull;
import cm.aptoide.pt.spotandshare.socket.entities.AndroidAppInfo;
import cm.aptoide.pt.spotandshare.socket.interfaces.FileLifecycleProvider;
import cm.aptoide.pt.spotandshare.socket.interfaces.OnError;
import cm.aptoide.pt.spotandshare.socket.interfaces.SocketBinder;
import cm.aptoide.pt.spotandshare.socket.message.interfaces.AndroidAppInfoAccepter;
import cm.aptoide.pt.spotandshare.socket.message.interfaces.StorageCapacity;
import java.io.IOException;
import lombok.Getter;

/**
 * Created by neuro on 10-07-2017.
 */
@Getter public class MessageServerConfiguration {

  private final Context context;
  private final String externalStoragepath;
  private final StorageCapacity storageCapacity;
  private final FileLifecycleProvider<AndroidAppInfo> fileLifecycleProvider;
  private final SocketBinder socketBinder;
  private final OnError<IOException> onError;
  private final AndroidAppInfoAccepter androidAppInfoAccepter;

  public MessageServerConfiguration(Context context, OnError<IOException> onError,
      AndroidAppInfoAccepter androidAppInfoAccepter) {
    this.context = context;
    this.externalStoragepath =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            .toString();
    this.storageCapacity = newStorageCapacity(externalStoragepath);
    this.fileLifecycleProvider = new FileLifecycleProviderHelper().newFileLifecycleProvider();
    this.socketBinder = newDefaultSocketBinder();
    this.onError = onError;
    this.androidAppInfoAccepter = androidAppInfoAccepter;
  }

  public SocketBinder newDefaultSocketBinder() {
    return socket -> {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        ConnectivityManager conMgr =
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network[] networks = conMgr.getAllNetworks();
        if (networks != null) {
          for (Network network : networks) {
            NetworkInfo networkInfo = conMgr.getNetworkInfo(network);
            if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
              if (networkInfo.isAvailable() && networkInfo.isConnected()) {
                try {
                  network.bindSocket(socket);
                } catch (IOException e) {
                  e.printStackTrace();
                }
              }
            }
          }
        }
      }
    };
  }

  @NonNull private StorageCapacity newStorageCapacity(String externalStoragepath) {
    return bytes -> {
      long availableSpace = -1L;
      StatFs stat = new StatFs(externalStoragepath);
      availableSpace = (long) stat.getAvailableBlocks() * (long) stat.getBlockSize();
      return availableSpace > bytes;
    };
  }
}