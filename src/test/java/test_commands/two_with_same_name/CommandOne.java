package test_commands.two_with_same_name;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import com.mdanetzky.streamscript.parser.annotations.Command;
import com.mdanetzky.streamscript.parser.annotations.CommandParameters;
import com.mdanetzky.streamscript.parser.annotations.StreamSource;

@Command("sameCommand")
public class CommandOne {

    @SuppressWarnings({"SameReturnValue", "unused"})
    @StreamSource
    public Source<ByteString, NotUsed> source() {
        return null;
    }

    @SuppressWarnings({"EmptyMethod", "unused"})
    @CommandParameters
    public void setCode(String Code) {
    }
}
