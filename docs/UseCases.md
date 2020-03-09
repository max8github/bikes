# Notable Use Cases
## ErrorState Use Case
 `object Timeout`  
 generates:  
 `ErrorEvent(errorMessage: String, previousState: State, causeCommand: Command)`  
 which  
 `ErrorState(errorMsg, command, errState)`
 Now, the only thing that can get the FSM out of that error state is  
 `KickEvent(previousState)`  
 which is created off of a  
 `object KickCmd`  
 The "kick" will pick up the `offendingCommand`(=`commandToReIssue`) and `previousState` from the error state,
 create a `KickEvent(previousState)` first (`with persist()`) and then update the tree and then send the command to re-issue.
 The `KickEvent(previousState)` will make the FSM transition to the new state `previousState`, ready to accept the commands.
 
 Note that the `KickCmd` command is sent by the user in case of server issues, like a service momentarily unavailable.
 If there is something wrong with user input, like a wrong blueprint or wrong credentials, the action to take is to 
 restart, not to kick.  
 In case of wrong credentials, the fsm will be stuck in its `InitState`, so, has to be restarted.  
 In case of problems with the blueprint, the blueprint is immutable (NI-URI), so, a new entity must be created.
