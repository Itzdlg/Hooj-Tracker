package sh.dominick.inpeel.rest.sessions

import sh.dominick.inpeel.lib.managers.SessionManager

object RestSessionManager : SessionManager<SessionTransformer>("session")