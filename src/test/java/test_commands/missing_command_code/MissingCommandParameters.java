package test_commands.missing_command_code;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import com.mdanetzky.streamscript.parser.annotations.Command;
import com.mdanetzky.streamscript.parser.annotations.StreamSource;

@SuppressWarnings("unused")
@Command("")
public class MissingCommandParameters {

    @StreamSource
    public Source<ByteString, NotUsed> source() {
        return null;
    }
}
