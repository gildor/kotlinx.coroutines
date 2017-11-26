package kotlinx.coroutines.experimental.retrofit;

import com.sun.tools.internal.xjc.reader.xmlschema.bindinfo.BIConversion;
import retrofit2.Call;

public interface TestJava {

    Call<BIConversion.User> getUser();

    Call<BIConversion.User> getUserNullable();

}
