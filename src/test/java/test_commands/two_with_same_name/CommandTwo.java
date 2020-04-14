package test_commands.two_with_same_name;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import com.mdanetzky.streamscript.parser.annotations.Command;
import com.mdanetzky.streamscript.parser.annotations.CommandParameters;
import com.mdanetzky.streamscript.parser.annotations.StreamSource;

@SuppressWarnings("unused")
@Command("sameCommand")
public class CommandTwo {

    @StreamSource
    public Source<ByteString, NotUsed> source() {
        return null;
    }

    @CommandParameters
    public void setCode(String Code) {
    }

}
