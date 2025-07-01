import com.google.gson.annotations.SerializedName

data class UserProfileResponse(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("phone_number") val phone: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("registration_date") val registrationDate: String? = null,
    @SerializedName("plan_type") val planType: String? = null,
    @SerializedName("plan_duration") val planDuration: Int? = null,
    @SerializedName("error") val error: String? = null
)
