package com.example.optimate.businessOwner
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.Card
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.MaterialTheme
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.OutlinedTextField
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.Scaffold
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.Switch
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optimate.R
import com.example.optimate.loginAndRegister.GlobalUserData
import com.google.firebase.firestore.FirebaseFirestore


private val managerAccessList = listOf(
    "Scheduling",
    "Finances",
    "View Employees",
    "Time-off Requests Approval"
)

private val employeeAccessList = listOf(
    "View Schedule",
    "Clock-in/out",
    "View Payroll",
    "Add Expense",
    "Availability"
)

private val managerBasicAccessList = listOf(
    "Scheduling",
    "View Employees",
    "Time-off Requests Approval"
)
private val employeeBasicAccessList = listOf(
    "View Schedule",
    "Availability",
)

data class TitleEntry(
    val bid: String,
    val role: String,
    val title: String,
    val access: List<String>
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SetupRoleUI(
    role: String, // "Manager" or "Employee"
    onAddComplete: () -> Unit
) {
    val context = LocalContext.current
    var titleText by remember { mutableStateOf("") }
    val selectedAccess = remember { mutableStateListOf<String>() }
    val managerList = if (GlobalUserData.modules.contains("Plus")) managerAccessList else managerBasicAccessList
    val employeeList = if (GlobalUserData.modules.contains("Plus")) employeeAccessList else employeeBasicAccessList

    Scaffold(
        topBar = { XmlTopBar(titleText = "Add $role".replaceFirstChar { it.uppercase() }) },
        backgroundColor = colorResource(id = R.color.screen_border_colors),
        content = { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TitleInput(titleText,{ titleText = it },modifier = Modifier.weight(2f))
                    Spacer(modifier = Modifier.width(8.dp))
                    AddButton(
                        onClick = {
                            addTitleToDB(
                                bid = GlobalUserData.bid,
                                role = role,
                                title = titleText,
                                access = selectedAccess.toList(),
                                context = context,
                                onSuccess = onAddComplete,
                                onFailure = { e ->
                                    Toast.makeText(context, "Failed to add title: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        context = context,
                        titleText = titleText,
                        modifier = Modifier.weight(1f)
                    )
                }

                LazyColumn(
                    modifier = Modifier.padding(top = 8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (role == "Manager") {
                        stickyHeader {
                            Text(
                                "Manager's Access:",
                                style = MaterialTheme.typography.h6,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colors.background)
                                    .padding(8.dp)
                            )
                        }
                        itemsIndexed(managerList) { index, access ->
                            AccessCardItem(access, index, role) { accessName, isSelected ->
                                if (isSelected) selectedAccess.add(accessName) else selectedAccess.remove(accessName)
                            }
                        }
                    }

                    stickyHeader {
                        Text(
                            "Employee's Access:",
                            style = MaterialTheme.typography.h6,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colors.background)
                                .padding(8.dp)
                        )
                    }
                    itemsIndexed(employeeList) { index, access ->
                        AccessCardItem(access, index, role) { accessName, isSelected ->
                            if (isSelected) selectedAccess.add(accessName) else selectedAccess.remove(accessName)
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun TitleInput(titleText: String, onTitleChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var text by remember { mutableStateOf(titleText) }

    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onTitleChange(it)
        },
        label = { Text("Title") },
        textStyle = TextStyle(fontSize = 18.sp),
        modifier = modifier
    )
}

@Composable
fun AddButton(onClick: () -> Unit, context: Context, titleText: String, modifier: Modifier = Modifier) {
    val buttonColor = colorResource(id = R.color.light_green)

    Button(
        colors = ButtonDefaults.run { buttonColors(buttonColor) },
        onClick = {
            // Check if titleText is not empty before proceeding
            if (titleText.isNotBlank()) {
                onClick()
            } else {
                // Show a Toast message if titleText is empty
                Toast.makeText(context, "Title cannot be empty", Toast.LENGTH_SHORT).show()
            }
        },
        modifier = modifier,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 8.dp)
    ) {
        Text("Add", fontSize = 15.sp, color = Color.Black, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
    }
}


@Composable
fun AccessCardItem(access: String, index: Int, role:String, onAccessSelected: (String, Boolean) -> Unit) {
    val backgroundColor = if (role == "Manager") colorResource(id = R.color.light_blue) else colorResource(id = R.color.light_red)
    var switchState by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(1.dp, backgroundColor, RoundedCornerShape(12.dp)),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = access, fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
            Switch(
                checked = switchState,
                onCheckedChange = {
                    switchState = it
                    onAccessSelected(access, it)
                }
            )
        }
    }
}

fun addTitleToDB(
    bid: String,
    role: String,
    title: String,
    access: List<String>,
    context: Context, // Pass the context from the activity or use LocalContext.current in a composable
    onSuccess: () -> Unit,
    onFailure: (Exception) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    // Query to check if the title already exists for the given BID
    db.collection("titles")
        .whereEqualTo("bid", bid)
        .whereEqualTo("title", title)
        .get()
        .addOnSuccessListener { documents ->
            if (documents.size() > 0) {
                // The title already exists for this BID
                Toast.makeText(context, "Title already exists", Toast.LENGTH_SHORT).show()
            } else {
                // The title does not exist, proceed to add it
                val newTitle = TitleEntry(bid, role, title, access)
                db.collection("titles")
                    .add(newTitle)
                    .addOnSuccessListener {
                        onSuccess()
                    }
                    .addOnFailureListener { e ->
                        onFailure(e)
                    }
            }
        }
        .addOnFailureListener { e ->
            // Handle the error, e.g., show an error message
            Toast.makeText(context, "Error checking title: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}





