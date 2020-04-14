package test_commands.no_default_constructor;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import com.mdanetzky.streamscript.parser.annotations.Command;
import com.mdanetzky.streamscript.parser.annotations.CommandParameters;
import com.mdanetzky.streamscript.parser.annotations.StreamSource;

@SuppressWarnings({"unused", "SameReturnValue"})
@Command("")
public class NoDefaultConstructorCommand {

    public NoDefaultConstructorCommand(String someData) {

    }

    @StreamSource
    public Source<ByteString, NotUsed> source() {
        return null;
    }

    @SuppressWarnings("EmptyMethod")
    @CommandParameters
    public void setCode(String Code) {
    }
}
