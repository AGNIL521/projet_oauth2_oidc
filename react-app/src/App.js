import React, { useState, useEffect } from "react";
import axios from "axios";

function App({ keycloak }) {
    const [products, setProducts] = useState([]);
    const [orders, setOrders] = useState([]);
    const [cart, setCart] = useState({});
    const [error, setError] = useState(null);
    const [activeTab, setActiveTab] = useState("products");
    const [newProduct, setNewProduct] = useState({ name: "", price: 0, quantity: 0, description: "" });

    const isAdmin = keycloak.tokenParsed?.realm_access?.roles?.includes("ADMIN");

    useEffect(() => {
        if (keycloak.authenticated) {
            fetchProducts();
        }
    }, [keycloak.authenticated]);

    const api = axios.create({
        baseURL: "http://localhost:8085",
    });

    api.interceptors.request.use(async (config) => {
        if (keycloak.isTokenExpired(5)) {
            await keycloak.updateToken(30);
        }
        config.headers.Authorization = `Bearer ${keycloak.token}`;
        return config;
    });

    const fetchProducts = async () => {
        try {
            const res = await api.get("/products");
            setProducts(res.data);
            setError(null);
        } catch (err) {
            handleError(err);
        }
    };

    const fetchOrders = async () => {
        try {
            const res = await api.get("/orders");
            setOrders(res.data);
            setError(null);
        } catch (err) {
            handleError(err);
        }
    };

    const handleError = (err) => {
        if (err.response) {
            if (err.response.status === 401) setError("Unauthorized (401)");
            else if (err.response.status === 403) setError("Access Denied (403)");
            else setError(`Error: ${err.response.status}`);
        } else {
            setError("Network Error");
        }
    };

    const addToCart = (product) => {
        setCart((prev) => ({
            ...prev,
            [product.id]: (prev[product.id] || 0) + 1,
        }));
    };

    const placeOrder = async () => {
        const orderLines = Object.keys(cart).map((productId) => ({
            productId: parseInt(productId),
            quantity: cart[productId],
        }));

        if (orderLines.length === 0) return;

        try {
            await api.post("/orders", { orderLines });
            alert("Order placed successfully!");
            setCart({});
            if (activeTab === "orders") fetchOrders();
        } catch (err) {
            handleError(err);
            alert("Failed to place order");
        }
    };

    const addProduct = async (e) => {
        e.preventDefault();
        try {
            await api.post("/products", newProduct);
            alert("Product added!");
            setNewProduct({ name: "", price: 0, quantity: 0, description: "" });
            fetchProducts();
        } catch (err) {
            handleError(err);
        }
    };

    const deleteProduct = async (id) => {
        if (!window.confirm("Are you sure?")) return;
        try {
            await api.delete(`/products/${id}`);
            alert("Product deleted!");
            fetchProducts();
        } catch (err) {
            handleError(err);
        }
    };

    return (
        <div style={{ padding: "20px", fontFamily: "Arial" }}>
            <header style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "20px" }}>
                <h1>Microservices Shop</h1>
                <div>
                    <span>{keycloak.tokenParsed?.preferred_username} </span>
                    <button onClick={() => keycloak.logout()}>Logout</button>
                </div>
            </header>

            {error && <div style={{ color: "red", marginBottom: "10px" }}>{error}</div>}

            <div style={{ marginBottom: "20px" }}>
                <button onClick={() => setActiveTab("products")}>Products</button>
                <button onClick={() => { setActiveTab("orders"); fetchOrders(); }} style={{ marginLeft: "10px" }}>My Orders</button>
                {isAdmin && <span style={{ marginLeft: "10px", color: "blue" }}>[ADMIN MODE]</span>}
            </div>

            {activeTab === "products" && (
                <div>
                    <h3>Catalog</h3>
                    {isAdmin && (
                        <div style={{ marginBottom: "20px", border: "1px solid blue", padding: "10px", borderRadius: "5px" }}>
                            <h4>Add New Product (Admin)</h4>
                            <form onSubmit={addProduct} style={{ display: "flex", gap: "10px" }}>
                                <input placeholder="Name" value={newProduct.name} onChange={e => setNewProduct({ ...newProduct, name: e.target.value })} required />
                                <input placeholder="Price" type="number" value={newProduct.price} onChange={e => setNewProduct({ ...newProduct, price: parseFloat(e.target.value) })} required />
                                <input placeholder="Quantity" type="number" value={newProduct.quantity} onChange={e => setNewProduct({ ...newProduct, quantity: parseInt(e.target.value) })} required />
                                <input placeholder="Description" value={newProduct.description} onChange={e => setNewProduct({ ...newProduct, description: e.target.value })} />
                                <button type="submit">Add</button>
                            </form>
                        </div>
                    )}
                    <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(200px, 1fr))", gap: "20px" }}>
                        {products.map((p) => (
                            <div key={p.id} style={{ border: "1px solid #ccc", padding: "10px", borderRadius: "5px" }}>
                                <h4>{p.name}</h4>
                                <p>{p.description}</p>
                                <p>Price: ${p.price}</p>
                                <p>Stock: {p.quantity}</p>
                                <button onClick={() => addToCart(p)}>Add to Cart</button>
                                {isAdmin && (
                                    <button onClick={() => deleteProduct(p.id)} style={{ marginLeft: "10px", color: "red" }}>Delete</button>
                                )}
                            </div>
                        ))}
                    </div>

                    {Object.keys(cart).length > 0 && (
                        <div style={{ marginTop: "20px", borderTop: "2px solid #333", paddingTop: "10px" }}>
                            <h4>Cart</h4>
                            <ul>
                                {Object.keys(cart).map((pid) => (
                                    <li key={pid}>Product ID {pid}: {cart[pid]}</li>
                                ))}
                            </ul>
                            <button onClick={placeOrder} style={{ backgroundColor: "green", color: "white", padding: "10px" }}>
                                Place Order
                            </button>
                        </div>
                    )}
                </div>
            )}

            {activeTab === "orders" && (
                <div>
                    <h3>Orders</h3>
                    <table border="1" cellPadding="10" style={{ width: "100%", borderCollapse: "collapse" }}>
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Date</th>
                                <th>Status</th>
                                <th>Total</th>
                            </tr>
                        </thead>
                        <tbody>
                            {orders.map((o) => (
                                <tr key={o.id}>
                                    <td>{o.id}</td>
                                    <td>{o.date}</td>
                                    <td>{o.status}</td>
                                    <td>${o.totalAmount}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    );
}

export default App;
