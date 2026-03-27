import GoogleSignIn
import UIKit

class GoogleSignInHelper {

    static func signIn() async throws -> String {
        guard let rootVC = await UIApplication.shared.connectedScenes
            .compactMap({ $0 as? UIWindowScene })
            .first?.windows
            .first?.rootViewController else {
            throw AuthError.noRootViewController
        }

        let clientID = "344486764678-thhqkhe02a9gmneldu1gifm7jfp583jc.apps.googleusercontent.com"
        let config = GIDConfiguration(clientID: clientID)
        GIDSignIn.sharedInstance.configuration = config

        let result = try await GIDSignIn.sharedInstance.signIn(
            withPresenting: rootVC
        )

        guard let idToken = result.user.idToken?.tokenString else {
            throw AuthError.missingToken
        }

        return idToken
    }
}

enum AuthError: Error {
    case noRootViewController
    case missingToken
}
