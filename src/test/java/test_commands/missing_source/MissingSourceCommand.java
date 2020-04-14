package test_commands.missing_source;

import com.mdanetzky.streamscript.parser.annotations.Command;
import com.mdanetzky.streamscript.parser.annotations.CommandParameters;

@SuppressWarnings("unused")
@Command("")
public class MissingSourceCommand {

    @CommandParameters
    public void setCode(String Code) {
    }
}
